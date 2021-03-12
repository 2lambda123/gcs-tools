package magnolify.tools;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.avro.Schema;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.schema.MessageType;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

public class MagnolifyParquetTool implements Tool {
  @Override
  public String getName() {
    return "parquet";
  }

  @Override
  public String getShortDescription() {
    return "Generate Magnolify compatible Scala types from a Parquet file";
  }

  @Override
  public int run(InputStream in, PrintStream out, PrintStream err, List<String> args) throws Exception {
    OptionParser optionParser = new OptionParser();
    OptionSpec<Integer> widthOption = optionParser
        .accepts("width", "Column width")
        .withOptionalArg()
        .ofType(Integer.class)
        .defaultsTo(80);
    OptionSpec<Void> sourceOption = optionParser
        .accepts("source", "Include source schema.");
    OptionSpec<Void> avroOption = optionParser
        .accepts("avro", "Use Avro schema if available.");

    OptionSet optionSet = optionParser.parse(args.toArray(new String[0]));
    int width = optionSet.valueOf(widthOption);
    boolean source = optionSet.has(sourceOption);
    boolean avro = optionSet.has(avroOption);
    @SuppressWarnings("unchecked")
    List<String> argz = (List<String>) optionSet.nonOptionArguments();
    if (argz.size() != 1) {
      err.println("parquet --width <width> --avro --source input-file");
      err.println();
      err.println(getShortDescription());
      return 1;
    }

    Path path = new Path(argz.get(0));
    ParquetFileReader reader = ParquetFileReader
        .open(HadoopInputFile.fromPath(path, new Configuration()));
    Map<String, String> meta = reader.getFileMetaData().getKeyValueMetaData();

    String sourceStr;
    Record schema;
    if (avro) {
      String model = meta.get("writer.model.name");
      if (!"avro".equals(model)) {
        err.println("File metadata writer.model.name != avro: " + model);
        return 1;
      }
      String schemaString = meta.get("parquet.avro.schema");
      if (schemaString == null) {
        err.println("Missing Avro schema");
        return 1;
      }

      Schema avroSchema = new Schema.Parser().parse(schemaString);
      sourceStr = avroSchema.toString(true);
      schema = AvroParser.parse(avroSchema);
    } else {
      MessageType parquetSchema = reader.getFileMetaData().getSchema();
      sourceStr = parquetSchema.toString();
      schema = ParquetParser.parse(parquetSchema);
    }

    out.println("// Generated by magnolify-tools ");
    out.println("// Source: " + path);
    out.println();

    if (source) {
      out.println("/*");
      out.println(sourceStr);
      out.println("*/");
      out.println();
    }

    out.println(SchemaPrinter.print(schema, width));
    return 0;
  }
}

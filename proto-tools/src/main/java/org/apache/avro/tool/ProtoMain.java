/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.avro.tool;

import io.github.pixee.security.BoundedLineReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import java.io.InputStream;

/** Command-line driver.*/
public class ProtoMain {
  /**
   * Available tools, initialized in constructor.
   */
  final Map<String, Tool> tools;

  int maxLen = 0;

  ProtoMain() {
    tools = new TreeMap<String, Tool>();
    for (Tool tool : new Tool[] {
        new ProtoGetSchemaTool(),
        new ProtoToJsonTool()
    }) {
      Tool prev = tools.put(tool.getName(), tool);
      if (prev != null) {
        throw new AssertionError(
            "Two tools with identical names: " + tool + ", " + prev);
      }
      maxLen = Math.max(tool.getName().length(), maxLen);
    }
  }

  public static void main(String[] args) throws Exception {
    int rc = new ProtoMain().run(args);
    System.exit(rc);
  }

  /**
   * Delegates to tool specified on the command-line.
   */
  private int run(String[] args) throws Exception {
    if (args.length != 0) {
      Tool tool = tools.get(args[0]);
      if (tool != null) {
        return tool.run(
            System.in, System.out, System.err, Arrays.asList(args).subList(1, args.length));
      }
    }

    System.err.println("Protobuf Tools");
    System.err.println("----------------");

    System.err.println("Available tools:");
    for (Tool k : tools.values()) {
      System.err.printf("%" + maxLen + "s  %s\n", k.getName(), k.getShortDescription());
    }

    return 1;
  }

  private static void printStream(InputStream in) throws Exception {
    byte[] buffer = new byte[1024];
    for (int i = in.read(buffer); i != -1; i = in.read(buffer))
      System.err.write(buffer, 0, i);
  }

  private static void printHead(InputStream in, int lines) throws Exception {
    BufferedReader r = new BufferedReader(new InputStreamReader(in));
    for (int i = 0; i < lines; i++) {
      String line = BoundedLineReader.readLine(r, 5_000_000);
      if (line == null) {
        break;
      }
      System.err.println(line);
    }
  }

}

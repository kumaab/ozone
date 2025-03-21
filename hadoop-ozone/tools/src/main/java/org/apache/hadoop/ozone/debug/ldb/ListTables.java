/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.ozone.debug.ldb;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.hadoop.hdds.utils.db.RocksDatabase;
import picocli.CommandLine;

/**
 * List all column Families/Tables in db.
 */
@CommandLine.Command(
        name = "list_column_families",
        aliases = "ls",
        description = "list all column families in db."
)
public class ListTables implements Callable<Void> {

  @CommandLine.ParentCommand
  private RDBParser parent;

  @Override
  public Void call() throws Exception {
    List<byte[]> columnFamilies = RocksDatabase.listColumnFamiliesEmptyOptions(
        parent.getDbPath());
    for (byte[] b : columnFamilies) {
      System.out.println(new String(b, StandardCharsets.UTF_8));
    }
    return null;
  }
}

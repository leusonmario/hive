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

package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.metastore.MetaStoreUtils;
import org.apache.hadoop.hive.ql.exec.Utilities;
import org.apache.hadoop.hive.ql.io.AcidUtils;
import org.apache.hadoop.hive.ql.plan.Explain.Level;

/**
 * LoadTableDesc.
 *
 */
public class LoadTableDesc extends org.apache.hadoop.hive.ql.plan.LoadDesc
    implements Serializable {
  private static final long serialVersionUID = 1L;
  private boolean replace;
  private DynamicPartitionCtx dpCtx;
  private ListBucketingCtx lbCtx;
  private boolean inheritTableSpecs = true; //For partitions, flag controlling whether the current
                                            //table specs are to be used
  // Need to remember whether this is an acid compliant operation, and if so whether it is an
  // insert, update, or delete.
  private AcidUtils.Operation writeType;
  private Long txnId;
  private int stmtId;

  // TODO: the below seems like they should just be combined into partitionDesc
  private org.apache.hadoop.hive.ql.plan.TableDesc table;
  private Map<String, String> partitionSpec; // NOTE: this partitionSpec has to be ordered map
  private boolean commitMmWriteId = true;

  public LoadTableDesc(final LoadTableDesc o) {
    super(o.getSourcePath());

    this.replace = o.replace;
    this.dpCtx = o.dpCtx;
    this.lbCtx = o.lbCtx;
    this.inheritTableSpecs = o.inheritTableSpecs;
    this.writeType = o.writeType;
    this.table = o.table;
    this.partitionSpec = o.partitionSpec;
  }

  public LoadTableDesc(final Path sourcePath,
      final org.apache.hadoop.hive.ql.plan.TableDesc table,
      final Map<String, String> partitionSpec,
      final boolean replace,
      final AcidUtils.Operation writeType, Long txnId) {
    super(sourcePath);
    Utilities.LOG14535.info("creating part LTD from " + sourcePath + " to "
        + ((table.getProperties() == null) ? "null" : table.getTableName()));
    init(table, partitionSpec, replace, writeType, txnId);
  }

  /**
   * For use with non-ACID compliant operations, such as LOAD
   * @param sourcePath
   * @param table
   * @param partitionSpec
   * @param replace
   */
  public LoadTableDesc(final Path sourcePath,
                       final TableDesc table,
                       final Map<String, String> partitionSpec,
                       final boolean replace,
                       final Long txnId) {
    this(sourcePath, table, partitionSpec, replace, AcidUtils.Operation.NOT_ACID, txnId);
  }

  public LoadTableDesc(final Path sourcePath,
      final org.apache.hadoop.hive.ql.plan.TableDesc table,
      final Map<String, String> partitionSpec,
      final AcidUtils.Operation writeType, Long txnId) {
    this(sourcePath, table, partitionSpec, true, writeType, txnId);
  }

  /**
   * For DDL operations that are not ACID compliant.
   * @param sourcePath
   * @param table
   * @param partitionSpec
   */
  public LoadTableDesc(final Path sourcePath,
                       final org.apache.hadoop.hive.ql.plan.TableDesc table,
                       final Map<String, String> partitionSpec, Long txnId) {
    this(sourcePath, table, partitionSpec, true, AcidUtils.Operation.NOT_ACID, txnId);
  }

  public LoadTableDesc(final Path sourcePath,
      final org.apache.hadoop.hive.ql.plan.TableDesc table,
      final DynamicPartitionCtx dpCtx,
      final AcidUtils.Operation writeType,
      boolean isReplace, Long txnId) {
    super(sourcePath);
    Utilities.LOG14535.info("creating LTD from " + sourcePath + " to " + table.getTableName()/*, new Exception()*/);
    this.dpCtx = dpCtx;
    if (dpCtx != null && dpCtx.getPartSpec() != null && partitionSpec == null) {
      init(table, dpCtx.getPartSpec(), isReplace, writeType, txnId);
    } else {
      init(table, new LinkedHashMap<String, String>(), isReplace, writeType, txnId);
    }
  }

  private void init(
      final org.apache.hadoop.hive.ql.plan.TableDesc table,
      final Map<String, String> partitionSpec,
      final boolean replace,
      AcidUtils.Operation writeType, Long txnId) {
    this.table = table;
    this.partitionSpec = partitionSpec;
    this.replace = replace;
    this.writeType = writeType;
    this.txnId = txnId;
  }

  @Explain(displayName = "table", explainLevels = { Level.USER, Level.DEFAULT, Level.EXTENDED })
  public TableDesc getTable() {
    return table;
  }

  public void setTable(final org.apache.hadoop.hive.ql.plan.TableDesc table) {
    this.table = table;
  }

  @Explain(displayName = "partition")
  public Map<String, String> getPartitionSpec() {
    return partitionSpec;
  }

  public void setPartitionSpec(final Map<String, String> partitionSpec) {
    this.partitionSpec = partitionSpec;
  }

  @Explain(displayName = "replace")
  public boolean getReplace() {
    return replace;
  }

  @Explain(displayName = "micromanaged table")
  public Boolean isMmTableExplain() {
    return isMmTable() ? true : null;
  }

  public boolean isMmTable() {
    return MetaStoreUtils.isInsertOnlyTable(table.getProperties());
  }

  public void setReplace(boolean replace) {
    this.replace = replace;
  }

  public DynamicPartitionCtx getDPCtx() {
    return dpCtx;
  }

  public void setDPCtx(final DynamicPartitionCtx dpCtx) {
    this.dpCtx = dpCtx;
  }

  public boolean getInheritTableSpecs() {
    return inheritTableSpecs;
  }

  public void setInheritTableSpecs(boolean inheritTableSpecs) {
    this.inheritTableSpecs = inheritTableSpecs;
  }

  /**
   * @return the lbCtx
   */
  public ListBucketingCtx getLbCtx() {
    return lbCtx;
  }

  /**
   * @param lbCtx the lbCtx to set
   */
  public void setLbCtx(ListBucketingCtx lbCtx) {
    this.lbCtx = lbCtx;
  }

  public AcidUtils.Operation getWriteType() {
    return writeType;
  }

  public Long getTxnId() {
    return txnId;
  }

  public void setTxnId(Long txnId) {
    this.txnId = txnId;
  }

  public int getStmtId() {
    return stmtId;
  }

  public void setStmtId(int stmtId) {
    this.stmtId = stmtId;
  }

  public void setIntermediateInMmWrite(boolean b) {
    this.commitMmWriteId = !b;
  }

  public boolean isCommitMmWrite() {
    return commitMmWriteId;
  }
}

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

package org.apache.hadoop.ozone.om.request.snapshot;

import static org.apache.hadoop.ozone.om.exceptions.OMException.ResultCodes.FILE_NOT_FOUND;

import java.io.IOException;
import org.apache.hadoop.hdds.utils.db.cache.CacheKey;
import org.apache.hadoop.hdds.utils.db.cache.CacheValue;
import org.apache.hadoop.ozone.om.OMMetadataManager;
import org.apache.hadoop.ozone.om.OMMetrics;
import org.apache.hadoop.ozone.om.OzoneManager;
import org.apache.hadoop.ozone.om.exceptions.OMException;
import org.apache.hadoop.ozone.om.execution.flowcontrol.ExecutionContext;
import org.apache.hadoop.ozone.om.helpers.SnapshotInfo;
import org.apache.hadoop.ozone.om.request.OMClientRequest;
import org.apache.hadoop.ozone.om.request.util.OmResponseUtil;
import org.apache.hadoop.ozone.om.response.OMClientResponse;
import org.apache.hadoop.ozone.om.response.snapshot.OMSnapshotSetPropertyResponse;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.OMRequest;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.SnapshotSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates the exclusive size of the snapshot.
 */
public class OMSnapshotSetPropertyRequest extends OMClientRequest {
  private static final Logger LOG =
      LoggerFactory.getLogger(OMSnapshotSetPropertyRequest.class);

  public OMSnapshotSetPropertyRequest(OMRequest omRequest) {
    super(omRequest);
  }

  @Override
  public OMClientResponse validateAndUpdateCache(OzoneManager ozoneManager, ExecutionContext context) {
    OMMetrics omMetrics = ozoneManager.getMetrics();

    OMClientResponse omClientResponse;
    OMMetadataManager metadataManager = ozoneManager.getMetadataManager();

    OzoneManagerProtocolProtos.OMResponse.Builder omResponse =
        OmResponseUtil.getOMResponseBuilder(getOmRequest());
    OzoneManagerProtocolProtos.SetSnapshotPropertyRequest
        setSnapshotPropertyRequest = getOmRequest()
        .getSetSnapshotPropertyRequest();

    String snapshotKey = setSnapshotPropertyRequest.getSnapshotKey();

    try {
      SnapshotInfo updatedSnapInfo = metadataManager.getSnapshotInfoTable().get(snapshotKey);
      if (updatedSnapInfo == null) {
        LOG.error("Snapshot: '{}' doesn't not exist in snapshot table.", snapshotKey);
        throw new OMException("Snapshot: '{" + snapshotKey + "}' doesn't not exist in snapshot table.", FILE_NOT_FOUND);
      }


      if (setSnapshotPropertyRequest.hasDeepCleanedDeletedDir()) {
        updatedSnapInfo.setDeepCleanedDeletedDir(setSnapshotPropertyRequest
            .getDeepCleanedDeletedDir());
      }

      if (setSnapshotPropertyRequest.hasDeepCleanedDeletedKey()) {
        updatedSnapInfo.setDeepClean(setSnapshotPropertyRequest
            .getDeepCleanedDeletedKey());
      }

      if (setSnapshotPropertyRequest.hasSnapshotSize()) {
        SnapshotSize snapshotSize = setSnapshotPropertyRequest
            .getSnapshotSize();
        long exclusiveSize = updatedSnapInfo.getExclusiveSize() +
            snapshotSize.getExclusiveSize();
        long exclusiveReplicatedSize = updatedSnapInfo
            .getExclusiveReplicatedSize() + snapshotSize
            .getExclusiveReplicatedSize();
        // Set Exclusive size.
        updatedSnapInfo.setExclusiveSize(exclusiveSize);
        updatedSnapInfo.setExclusiveReplicatedSize(exclusiveReplicatedSize);
      }
      // Update Table Cache
      metadataManager.getSnapshotInfoTable().addCacheEntry(
          new CacheKey<>(snapshotKey),
          CacheValue.get(context.getIndex(), updatedSnapInfo));
      omClientResponse = new OMSnapshotSetPropertyResponse(
          omResponse.build(), updatedSnapInfo);
      omMetrics.incNumSnapshotSetProperties();
      LOG.info("Successfully executed snapshotSetPropertyRequest: {{}}.", setSnapshotPropertyRequest);
    } catch (IOException ex) {
      omClientResponse = new OMSnapshotSetPropertyResponse(
          createErrorOMResponse(omResponse, ex));
      omMetrics.incNumSnapshotSetPropertyFails();
      LOG.error("Failed to execute snapshotSetPropertyRequest: {{}}.", setSnapshotPropertyRequest, ex);
    }

    return omClientResponse;
  }
}

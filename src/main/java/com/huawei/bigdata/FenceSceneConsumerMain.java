package com.huawei.bigdata;

import com.huaweicloud.dis.DISClient;
import com.huaweicloud.dis.core.util.StringUtils;
import com.huaweicloud.dis.iface.data.request.GetPartitionCursorRequest;
import com.huaweicloud.dis.iface.data.request.GetRecordsRequest;
import com.huaweicloud.dis.iface.data.response.GetPartitionCursorResult;
import com.huaweicloud.dis.iface.data.response.GetRecordsResult;
import com.huaweicloud.dis.iface.data.response.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * 消费DIS数据
 */
public class FenceSceneConsumerMain {
    private static final Logger LOG = LoggerFactory.getLogger(FenceSceneConsumerMain.class);

    public static void main(String args[]) throws IOException {
        // 获取进入电子围栏的车辆的信息
        getData();
    }

    /**
     * 获取CloudStream处理完成后的结果
     */
    public static void getData() {
        DISClient dic = new DISClient(Constants.DIS_CONFIG);
        GetPartitionCursorRequest request = new GetPartitionCursorRequest();

        GetRecordsRequest recordsRequest = new GetRecordsRequest();

        String streamName = Constants.OUTPUT_STREAM_NAME;
        if (StringUtils.isNullOrEmpty(streamName)) {
            streamName = Constants.INPUT_STREAM_NAME;
        }
        request.setStreamName(streamName);

        request.setPartitionId(String.valueOf(Constants.CONSUMER_PARTITION_ID));

        if (Constants.CONSUMER_SEQUENCE_NUMBER > -1) {
            request.setStartingSequenceNumber(String.valueOf(Constants.CONSUMER_SEQUENCE_NUMBER));
            request.setCursorType("AT_SEQUENCE_NUMBER");
        } else if (Constants.CONSUMER_SEQUENCE_NUMBER == -1) {
            request.setCursorType("LATEST");
        } else {
            request.setCursorType("TRIM_HORIZON");
        }

        GetPartitionCursorResult response = dic.getPartitionCursor(request);

        String iterator = response.getPartitionCursor();

        GetRecordsResult recordResponse = null;

        while (true) {

            recordsRequest.setPartitionCursor(iterator);

            recordResponse = dic.getRecords(recordsRequest);

            iterator = recordResponse.getNextPartitionCursor();

            for (Record record : recordResponse.getRecords()) {

                if ("normal".equalsIgnoreCase(Constants.CONSUMER_DISPLAY_FORMAT)) {
                    try {
                        LOG.info(new String(record.getData().array(), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        LOG.error(e.getMessage(), e);
                    }
                } else {
                    String[] results = new String(record.getData().array()).split(",");

                    if (results.length == 7) {

                        LOG.info(String.format("车辆：%s进入电子围栏区域，速度为%s，所在位置：%s,所在位置的百度经纬度：%s,%s,数据时间：%s",
                                results[0], results[5], results[4], results[2], results[3], results[1]));

                    }
                }
            }

            if (recordResponse.getRecords().size() == 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

}

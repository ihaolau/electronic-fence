package com.huawei.bigdata.filemonitor;

import com.huawei.bigdata.Constants;
import com.huawei.bigdata.dis.DeviceSendService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * 自定义文件监听器
 *
 * @author
 * @date
 * @file
 */

public class DataReaderListener extends FileAlterationListenerAdaptor {

    private static final Logger LOG = LoggerFactory.getLogger(DataReaderListener.class);
    private static Map<String, List<String>> dataMap = new HashMap<>();
    String dataDir;
    int deviceNum;

    public DataReaderListener() {

    }

    public DataReaderListener(String dataDir, int deviceNum) {
        this.dataDir = dataDir;
        this.deviceNum = deviceNum;

    }

    @Override
    public void onFileCreate(File file) {
        LOG.info("file {} has alread added,begin re-init memory store......", file.getName());
        getFileAndContent();
        DeviceSendService.isUpdate = true;
    }

    @Override
    public void onFileChange(File file) {
        LOG.info("file {} has alread update,begin re-init memory store......", file.getName());
        getFileAndContent();
        DeviceSendService.isUpdate = true;
    }

    @Override
    public void onFileDelete(File file) {
        LOG.info("file {} has alread deleted,begin re-init memory store......", file.getName());
        getFileAndContent();
        DeviceSendService.isUpdate = true;
    }

//	private void getFileAndContent(File file) {
//		try {
//			String key = StringUtils.split(file.getName(), ".")[0];
//			LOG.info("key is {}", key);
//			List<String> content = FileUtils.readLines(file);
//			
//			dataMap.put(key, content);
//		} catch (IOException e) {
//			LOG.error("read file {} failed!!!!!!!, {}", file.getPath(), e);
//		}
//	
//	}

    public void getFileAndContent() {
        if (!dataMap.isEmpty()) {
            dataMap.clear();
        }

        List<File> originFiles = new ArrayList<>(FileUtils.listFiles(new File(dataDir), new String[]{Constants.EXTEND_NAME}, true));
        LOG.info("files size is {}.", originFiles.size());
        for (File f : originFiles) {
            try {
                String key = StringUtils.split(f.getName(), ".")[0];
                LOG.info("key is {}", key);
                List<String> content = FileUtils.readLines(f, Charset.forName("UTF-8"));
                dataMap.put(key, content);
            } catch (IOException e) {
                LOG.error("read file {} failed!!!!!!!, {}", f.getPath(), e);
            }
        }
    }

    public List<String> getDeviceIDs() {
        List<String> deviceIDs = new ArrayList<>();
        deviceIDs.addAll(dataMap.keySet());
        return deviceIDs;
    }

    public List<String> getDeviceIDsBySize() {
        List<String> idList = getDeviceIDs();
        Collections.sort(idList, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return dataMap.get(o2).size() - dataMap.get(o1).size();
            }
        });
        return idList;
    }

    public List<String> getWholeData(String key) {
        return dataMap.get(key);
    }
}
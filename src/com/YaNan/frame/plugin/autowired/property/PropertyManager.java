package com.YaNan.frame.plugin.autowired.property;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;

import com.YaNan.frame.plugin.Environment;
import com.YaNan.frame.utils.resource.AbstractResourceEntry;
import com.YaNan.frame.utils.resource.Path;
import com.YaNan.frame.utils.resource.ResourceManager;
import com.typesafe.config.Config;

public class PropertyManager {
	private static volatile PropertyManager manager;
	private Map<String,String> propertyPools;
	private PropertyManager(){
		propertyPools  = new HashMap<String,String>();
		scanAllProperty();
		initSystemPropertyPools();
	}
	/**
	 * initial system property
	 */
	private void initSystemPropertyPools() {
		Properties properties = System.getProperties();
		Iterator<Entry<Object,Object>> entryIterator = properties.entrySet().iterator();
		while (entryIterator.hasNext()) {
			Entry<Object,Object> entry = entryIterator.next();
			propertyPools.put(Objects.toString(entry.getKey()), Objects.toString(entry.getValue()));
		}
	}
	/**
	 * get instance
	 * @return
	 */
	public static PropertyManager getInstance(){
		if(manager==null)
			synchronized (PropertyManager.class) {
				if(manager == null) {
					manager = new PropertyManager();
				}
			}
		return manager;
	}
	/**
	 * put property to pools
	 * @param name
	 * @param value
	 * @return
	 */
	public String setProperty(String name,Object value){
		return this.propertyPools.put(name, Objects.toString(value));
	}
	/**
	 * get property from pools
	 * @param name
	 * @return
	 */
	public String getProperty(String name){
		return this.propertyPools.get(name);
	}
	/**
	 * scan property files to pools
	 */
	public void scanAllProperty(){
		Properties properties = new Properties();
		//propertyScan
		List<AbstractResourceEntry> scanPaths = getScanResourceByConfig();
		if(scanPaths != null )
		scanPaths.forEach((resource)->{
			try {
				properties.load(resource.getInputStream());
				properties.entrySet().forEach((entry) -> {
					if(entry.getValue()!=null)
						propertyPools.put(entry.getKey().toString(),entry.getValue().toString());
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		this.rebuild();
	}
	private List<AbstractResourceEntry> getScanResourceByConfig() {
		Config config = Environment.getEnviroment().getConfigure();
		List<AbstractResourceEntry> resourceList = null;
		String[] scanPathArrays = null ;
		//propertyScan
		if(config != null) {
			config.allowKeyNull();
			if(config.hasPath("propertyScan")) {
				String scanPaths = config.getString("propertyScan");
				scanPathArrays = scanPaths.split(",");
			}
			config.allowKeyNull(false);
		}
		if(scanPathArrays == null) {
			scanPathArrays = new String[]{ResourceManager.classPath()};
		}
		if(scanPathArrays != null && scanPathArrays.length >0)
		for(String scanPath : scanPathArrays) {
			if (scanPath != null && !scanPath.endsWith(".properties")) {
				scanPath = scanPath+"**.properties";
			}
			List<AbstractResourceEntry> resourceEntryList = 
					ResourceManager.getResourceList(scanPath);
			if(resourceList == null) {
				resourceList = resourceEntryList;
			}else {
				resourceList.addAll(resourceEntryList);
			}
		}
		return resourceList;
	}
	@Deprecated
	public void scanPath(String scanPath) {
		File dir = new File(scanPath);
		Path path = new Path(dir);
		path.filter("**.properties");
		path.scanner((file) -> {
			Properties properties = new Properties();
			try {
				InputStream is = new FileInputStream(file);
				properties.load(is);
				properties.entrySet().forEach((entry) -> {
					if(entry.getValue()!=null)
						propertyPools.put(entry.getKey().toString(),entry.getValue().toString());
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	public synchronized void rebuild(){
		Map<String,String> tempProp = propertyPools;
		propertyPools =new HashMap<String,String>();
		tempProp.entrySet().forEach((entry) -> {
			if(!propertyPools.containsKey(entry.getKey())) {
				propertyPools.put(entry.getKey(), getValues(entry.getValue(),tempProp));
			}
		});
	}
	private String getValues(String orginValue,Map<String,String> tempProp) {
		if(orginValue==null) {
			return null;
		}
		String tempKey;
		String tempValue;
		int index,endex = 0;
		while((index=orginValue.indexOf("${", endex))>-1
				&&(endex=orginValue.indexOf("}",index+2))>-1
				&&(tempKey=orginValue.substring(index+2, endex))!=null
				&&!tempKey.trim().equals("")){
			tempValue = propertyPools.get(tempKey);
			if(tempValue==null) {
				tempValue = tempProp.get(tempKey);
			}
			if(tempValue == null){
				endex = endex+1;
			}else{
				tempValue = getValues(tempValue,tempProp);
				propertyPools.put(tempKey, tempValue);
				orginValue = orginValue.substring(0,index)+tempValue+orginValue.substring(endex+1);
				endex = endex-tempKey.length()-2+tempValue.length();
			}
		}
		return orginValue;
	}
	public void put(Properties properties) {
		properties.entrySet().forEach((entry) -> {
			if(entry.getValue()!=null)
				propertyPools.put(entry.getKey().toString(),entry.getValue().toString());
		});
		this.rebuild();
	}
	
}

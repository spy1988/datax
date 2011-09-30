/**
 * (C) 2010-2011 Alibaba Group Holding Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 
 * version 2 as published by the Free Software Foundation. 
 * 
 */


package com.taobao.datax.common.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Hashtable;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.log4j.Logger;

import com.taobao.datax.common.plugin.ParamsKey;


public class DfsUtils {
	private static final Logger log = Logger.getLogger(DfsUtils.class);

	private static FileSystem fs;

	private static Map<String, Class<?>> typeMap = null;

	static {
		typeMap = new HashMap<String, Class<?>>();
		typeMap.put("org.apache.hadoop.io.BooleanWritable", boolean.class);
		typeMap.put("org.apache.hadoop.io.ByteWritable", byte.class);
		typeMap.put("org.apache.hadoop.io.IntWritable", int.class);
		typeMap.put("org.apache.hadoop.io.VIntWritable", int.class);
		typeMap.put("org.apache.hadoop.io.LongWritable", long.class);
		typeMap.put("org.apache.hadoop.io.VLongWritable", long.class);
		typeMap.put("org.apache.hadoop.io.DoubleWritable", double.class);
		typeMap.put("org.apache.hadoop.io.FloatWritable", float.class);
		typeMap.put("org.apache.hadoop.io.Text", String.class);
	}

	private DfsUtils() {
	}
	
	public static Map<String, Class<?>> getTypeMap() {
		return typeMap;
	}

	public enum HdfsFileType {
		TXT, COMP_TXT, SEQ,
	}

	private static Hashtable<String, Configuration> confs = new Hashtable<String, Configuration>();

	/**
	 * Get {@link Configuration}.
	 * 
	 * @param	dir
	 * 			directory path in hdfs
	 * 
	 * @param	ugi
	 * 			hadoop ugi
	 * 
	 * @param	conf
	 * 			hadoop-site.xml path 
	 * 
	 * @return		
	 * 			{@link Configuration}
	 * 
	 * */
	public static Configuration getConf(String dir, String ugi, String conf)
			throws IOException {

		URI uri = null;
		Configuration cfg = new Configuration();
		String scheme = null;
		try {
			uri = new URI(dir);
			scheme = uri.getScheme();
			cfg = confs.get(scheme);
		} catch (URISyntaxException e) {
			throw new IOException(e.getMessage(), e.getCause());
		}

		if (cfg == null) {
			cfg = new Configuration();
			
			String confPath = "";
			
			if (!StringUtils.isBlank(conf) && 
					new File(conf).exists()) {
				confPath = conf;
			} else {
				/* For taobao internal use 
				 * e.g. if bazhen.csy start a new datax job, 
				 * datax will use /home/bazhen.csy/config/hadoop-site.xml as configuration xml
				 * */
				confPath =  System.getProperty("user.home")
						+ "/config/hadoop-site.xml";
			}
	
			cfg.addResource(new Path(confPath));
			
			log.info(String.format("HdfsReader use %s for hadoop configuration .", confPath));
			log.info("HdfsReader use default ugi " + cfg.get(ParamsKey.HdfsReader.ugi));

			if (uri.getScheme() != null) {
				String fsname = String.format("%s://%s:%s", uri.getScheme(),
						uri.getHost(), uri.getPort());
				log.info("fs.default.name=" + fsname);
				cfg.set("fs.default.name", fsname);
			}
			if (ugi != null) {
				cfg.set(ParamsKey.HdfsReader.ugi, ugi);
				log.info("use specification ugi:"
						+ cfg.get(ParamsKey.HdfsReader.ugi));
			}
			confs.put(scheme, cfg);
		}
		return cfg;
	}

	/**
	 * Get one handle of {@link FileSystem}.
	 * 
	 * @param	dir
	 * 			directory path in hdfs
	 * 
	 * @param	ugi
	 * 			hadoop ugi
	 * 
	 * @param	configure
	 * 			hadoop-site.xml path
	 * @return	
	 * 			one handle of {@link FileSystem}.
	 * */
	public static FileSystem getFileSystem(String dir, String ugi,
			String configure) throws IOException {
		System.out.flush();
		if (null == fs) {
			fs = FileSystem.get(getConf(dir, ugi, configure));
		}
		return fs;
	}

	
	/**
	 * Delete file specified by path or files in directory specified by path.
	 * 
	 * @param	path
	 * 			{@link Path} in hadoop
	 * 
	 * @param	flag
	 * 			need to do delete recursively
	 * 
	 * @param	isGlob
	 * 			need to use file pattern to match all files.
	 * 
	 * */
	public static void deleteFiles(Path path, boolean flag, boolean isGlob) {
		try {
			List<Path> paths = listDir(path, isGlob);
			for (Path p : paths) {
				deleteFile(p, flag);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Delete file specified by path or files in directory specified by path.
	 * 
	 * @param	dfs
	 * 			handle of {@link FileSystem}
	 * 
	 * @param	path
	 * 			{@link Path} in hadoop
	 * 
	 * @param	flag
	 * 			need to do delete recursively
	 * 
	 * @param	isGlob
	 * 			need to use file pattern to match all files.
	 * 
	 * */
	public static void deleteFiles(FileSystem dfs, Path path, boolean flag,
			boolean isGlob) {
		try {
			List<Path> paths = listDir(dfs, path, isGlob);
			for (Path p : paths) {
				deleteFile(dfs, p, flag);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 *List the statuses of the files/directories in the given path if the path is a directory.
	 * 
	 * @param	srcpath
	 * 			Path in {@link FileSystem}
	 * 
	 * @param	isGlob
	 * 			need to use file pattern
	 * 
	 * @return			
	 * 			all {@link Path} in srcpath.
	 * 
	 * */
	public static List<Path> listDir(Path srcpath, boolean isGlob) {
		List<Path> list = null;
		try {
			list = new ArrayList<Path>();
			FileStatus[] status = null;
			if (isGlob)
				status = fs.globStatus(srcpath);
			else
				status = fs.listStatus(srcpath);
			if (status != null) {
				for (FileStatus state : status) {
					list.add(state.getPath());
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}

	
	/**
	 *List the statuses of the files/directories in the given path if the path is a directory.
	 *
	 *@param	dfs
	 *			handle of {@link FileSystem}
	 * 
	 * @param	srcpath
	 * 			Path in {@link FileSystem}
	 * 
	 * @param	isGlob
	 * 			need to use file pattern
	 * 
	 * @return			
	 * 			all {@link Path} in srcpath
	 * 
	 * */
	public static List<Path> listDir(FileSystem dfs, Path srcpath,
			boolean isGlob) {
		List<Path> list = null;
		try {
			list = new ArrayList<Path>();
			FileStatus[] status = null;
			if (isGlob) {
				status = dfs.globStatus(srcpath);
			} else{
				status = dfs.listStatus(srcpath);
			}
			if (status != null) {
				for (FileStatus state : status) {
					list.add(state.getPath());
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * Delete file specified by path.
	 * 
	 * @param	path
	 * 			{@link Path} in hadoop
	 * 
	 * @param	flag
	 * 			need to do delete recursively
	 * 
	 * */
	public static void deleteFile(Path path, boolean flag) {
		try {
			log.debug("deleting:" + path.getName());
			fs.delete(path, flag);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Delete file specified by path.
	 * 
	 * @param	dfs
	 * 			handle of {@link FileSystem}
	 * 
	 * @param	path
	 * 			{@link Path} in hadoop
	 * 
	 * @param	flag
	 * 			need to do delete recursively
	 * 
	 * */
	public static void deleteFile(FileSystem dfs, Path path, boolean flag) {
		try {
			log.debug("deleting:" + path.getName());
			dfs.delete(path, flag);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Initialize handle of {@link FileSystem}.
	 * 
	 * @param	uri	
	 * 			URI
	 * 
	 * @param	conf
	 * 			{@link Configuration}
	 * 
	 * */
	public static FileSystem createFileSystem(URI uri, Configuration conf)
			throws IOException {
		Class<?> clazz = conf.getClass("fs." + uri.getScheme() + ".impl", null);
		if (clazz == null) {
			throw new IOException("No FileSystem for scheme: "
					+ uri.getScheme());
		}
		FileSystem fs = (FileSystem) ReflectionUtils.newInstance(clazz, conf);
		fs.initialize(uri, conf);
		return fs;
	}

	/**
	 * Check file type in hdfs.
	 * 
	 * @param	fs
	 * 			handle of {@link FileSystem}
	 * 
	 * @param	path
	 * 			hdfs {@link Path}
	 * 
	 * @param	conf
	 * 			{@link Configuration}
	 * 
	 * @return			
	 * 			{@link HdfsFileType}
	 * 			TXT, TXT_COMP, SEQ
	 * */
	public static HdfsFileType checkFileType(FileSystem fs, Path path,
			Configuration conf) throws IOException {
		FSDataInputStream is = null;
		try {
			is = fs.open(path);
			/* file is empty, use TXT readerup */
			if (0 == is.available()) {
				return HdfsFileType.TXT;
			}
			
			switch (is.readShort()) {
			case 0x5345:
				if (is.readByte() == 'Q') {
					// TODO: add RCFile
					return HdfsFileType.SEQ;
				}
			default:
				is.seek(0);
				CompressionCodecFactory compressionCodecFactory = new CompressionCodecFactory(
						conf);
				CompressionCodec codec = compressionCodecFactory.getCodec(path);
				if (null == codec)
					return HdfsFileType.TXT;
				else {
					return HdfsFileType.COMP_TXT;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		} finally {
			if (null != is)
				is.close();
		}
	}

}
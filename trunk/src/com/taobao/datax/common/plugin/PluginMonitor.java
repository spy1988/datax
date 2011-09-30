/**
 * (C) 2010-2011 Alibaba Group Holding Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 
 * version 2 as published by the Free Software Foundation. 
 * 
 */


package com.taobao.datax.common.plugin;

/**
 * Monitor monitors {@link Plugin} status, record number of lines, failed number of lines, etc.
 * 
 * @see {@link PluginStatus}
 * */
public interface PluginMonitor {
	
	/**
	 * Get number of successful lines.
	 * 
	 * @return
	 * 			number of successful lines.
	 * 
	 * */
	public long getSuccessedLines();
	
	/**
	 * Get number of failed lines.
	 * 
	 * @return
	 * 			number of failed lines.
	 * 
	 * */
	public long getFailedLines();
	
	/**
	 * Set number of successful lines.
	 * 
	 * @param	num	
	 * 			number of successful lines
	 * 
	 * @return
	 * 			0 for OK, others for failure.
	 * 
	 */
	public int setSuccessedLines(long num);
	
	/**
	 * Set number of failed lines.
	 * 
	 * @param	num	
	 * 			number of failed lines.
	 * 
	 * @return
	 * 			0 for OK, others for failure.
	 * 
	 */
	public int setFailedLines(long num);
	
	/**
	 * Increase the number of successful lines by step of one.
	 * 
	 * @return
	 * 			0 for OK, others for failure.
	 * 
	 * */
	public int lineSuccess();
	
	/**
	 * Increase the number of failed lines by 1.
	 * 
	 * @param	info
	 * 			failure information.
	 * 
	 * @return
	 * 			0 for OK, others for failure.
	 * 
	 * */
	public int lineFail(String info);
	
	/**
	 * Set status of {@link Plugin}.
	 * 
	 * @param	status
	 * 
	 * */
	public void setStatus(PluginStatus status);
	
	/**
	 * Get status of monitored {@link Plugin}.
	 * 
	 * @return
	 * 			{@link PluginStatus} which contains more details of the monitored plugin.
	 * 		
	 * */
	public PluginStatus getStatus();
	
	/**
	 * Get the monitored {@link Plugin} name.
	 * 
	 * @return
	 * 			name of the monitored {@link Plugin}.
	 * 
	 * */
	public String getTargetName();
	
	/**
	 *  Set the monitored {@link Plugin} name.
	 *  
	 *  @param	targetName
	 * 			name of the monitored {@link Plugin}.
	 * 
	 * */
	public void setTargetName(String targetName);
	
	/**
	 * Get the monitored {@link Plugin} id.
	 * 
	 * @return
	 * 			id of the monitored {@link Plugin}.
	 * 
	 * */
	public int getTargetId();
	
	/**
	 * Set the monitored {@link Plugin} id.
	 * 
	 * @param targetId
	 * 			id of the monitored {@link Plugin}.
	 * 
	 */
	public void setTargetId(int targetId);
	
}
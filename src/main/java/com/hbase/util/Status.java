package com.hbase.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.protobuf.generated.AdminProtos.GetRegionInfoResponse.CompactionState;
import org.apache.hadoop.hbase.util.Bytes;

public class Status {

	private static HBaseAdmin hbaseAdmin = null;
	private static Configuration conf = null;
	private static String useropt = "";
	private static final String OPTZOOKEEPERSERVER = "zookeeper-server";
	private static final String OPTZOOKEEPERPORT = "zookeeper-port";
	private static final String OPTZOOKEEPERZNODE = "zookeeper-znode";
	private static final String OPTTABLENAME = "table-name";

	public void getCompaction(String table) throws IOException {
		CompactionState state = null;
		List<HRegionInfo> regionListInTable = hbaseAdmin.getTableRegions(table.getBytes());
		int major = 0, minor = 0, major_and_minor = 0, none = 0, failed = 0;
		for (int j = 0; j < regionListInTable.size(); j++) {
			try {
				HRegionInfo regionInfo = regionListInTable.get(j);
				if (regionInfo != null) {
					state = hbaseAdmin.getCompactionState(regionListInTable.get(j).getRegionName());
					if (state == CompactionState.MAJOR) {
						major++;
					} else if (state == CompactionState.MINOR) {
						minor++;
					} else if (state == CompactionState.MAJOR_AND_MINOR) {
						major_and_minor++;
					} else {
						none++;
					}
				}
			} catch (Exception e) {
				failed++;
				continue;
			}
		}
		System.out.println("\n************************");
		System.out.println("Table        : " + table);
		System.out.println("Major        : " + major);
		System.out.println("Minor        : " + minor);
		System.out.println("Major_Minor  : " + major_and_minor);
		System.out.println("None         : " + none);
		System.out.println("TotalRegion  : " + regionListInTable.size());
		if (failed > 0) {
			System.out.println("StatusFailed : " + failed);
		}
	}

	public void getOnline(String table) throws IOException {
		System.out.println("\n************************\nTableName : " + table);
		List<HRegionInfo> regionListInTable = hbaseAdmin.getTableRegions(table.getBytes());
		for (HRegionInfo region : regionListInTable) {
			if (!region.isOffline()) {
				System.out.println(region.getRegionNameAsString());
			}
		}
	}

	public void getOffline(String table) throws IOException {
		System.out.println("\n************************\nTableName : " + table);
		List<HRegionInfo> regionListInTable = hbaseAdmin.getTableRegions(table.getBytes());
		for (HRegionInfo region : regionListInTable) {
			if (region.isOffline()) {
				System.out.println(region.getRegionNameAsString());
			}
		}
	}

	public void setConf(CommandLine cmdline) {
		if (cmdline.hasOption("z")) {
			conf.set(HConstants.ZOOKEEPER_QUORUM, cmdline.getOptionValue(OPTZOOKEEPERSERVER));
		}
		if (cmdline.hasOption("p")) {
			conf.set(HConstants.ZOOKEEPER_CLIENT_PORT, cmdline.getOptionValue(OPTZOOKEEPERPORT));
		}
		if (cmdline.hasOption("n")) {
			conf.set(HConstants.ZOOKEEPER_ZNODE_PARENT, cmdline.getOptionValue(OPTZOOKEEPERZNODE));
		}
	}

	public void optValidate(CommandLine cmdline, Options options, String cmd) {
		String commands[] = useropt.split(" ");
		if (cmd == null || !Arrays.asList(commands).contains(cmd)) {
			usage(options);
		}
	}

	public static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(
				"java -cp `hbase classpath` com.hbase.util.Status [--zookpeer-server <comma seperated list>] [--zookeper-znode <znode>] [--zookeeper-port <zk port>] [--table-name <all|tablename>] --cmd <compaction|onlineregion|offlineregion>  2>/tmp/_progress.log\n",
				options);
		System.exit(-1);
	}

	public static void main(String[] args) {
		Options options = new Options();
		try {
			Status status = new Status();
			CommandLineParser parser = new GnuParser();
			useropt = "compaction | onlineregion | offlineregion";
			options.addOption("z", OPTZOOKEEPERSERVER, true, "comma seperated zookeeper servers");
			options.addOption("p", OPTZOOKEEPERPORT, true, "zookeeper port");
			options.addOption("n", OPTZOOKEEPERZNODE, true, "zookeeper znode");
			options.addOption("t", OPTTABLENAME, true, "table name,for all tables --table-name=all");
			options.addOption("c", "cmd", true, "<" + useropt + ">");
			CommandLine line = parser.parse(options, args);
			String cmd = line.getOptionValue("cmd");
			status.optValidate(line, options, cmd);
			conf = HBaseConfiguration.create();
			status.setConf(line);
			hbaseAdmin = new HBaseAdmin(conf);
			String[] tablelist = null;
			if (!line.hasOption("t")) {
				tablelist = hbaseAdmin.getTableNames();
			} else if (line.getOptionValue(OPTTABLENAME).equalsIgnoreCase("all")) {
				tablelist = hbaseAdmin.getTableNames();
			} else {
				String tablename = line.getOptionValue(OPTTABLENAME);
				tablelist = new String[] { tablename };
				if (!hbaseAdmin.isTableAvailable(Bytes.toBytes(tablename))) {
					System.out.println("Table not found : " + tablename);
					System.exit(-1);
				}
			}
			if (cmd.equalsIgnoreCase("compaction")) {
				for (String table : tablelist) {
					status.getCompaction(table);
				}
			} else if (cmd.equalsIgnoreCase("onlineregion")) {
				for (String table : tablelist) {
					status.getOnline(table);
				}
			} else if (cmd.equalsIgnoreCase("offlineregion")) {
				for (String table : tablelist) {
					status.getOffline(table);
				}
			}
			hbaseAdmin.close();
		} catch (ParseException e) {
			usage(options);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(-1);
		}
	}
}

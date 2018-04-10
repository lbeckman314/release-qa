package org.reactome.qa.nullcheck;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.reactome.qa.QACheck;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.Report;

public class ReactionLikeEventChecker implements QACheck
{

	MySQLAdaptor dba;
	
	public void setAdaptor(MySQLAdaptor dba)
	{
		this.dba = dba;
	}

	private List<Long> getRLECompartmentSkipList() throws IOException {
		final String filePath = "src/main/resources/reaction_like_event_compartment_skip_list.txt";
		
		return getSkipList(filePath);
	}
	
	private List<Long> getRLEInputSkipList() throws IOException {
		final String filePath = "src/main/resources/reaction_like_event_input_skip_list.txt";
		
		return getSkipList(filePath);
	}
	
	private List<Long> getRLEOutputSkipList() throws IOException {
		final String filePath = "src/main/resources/reaction_like_event_output_skip_list.txt";
	
		return getSkipList(filePath);
	}
	
	private List<Long> getSkipList(String filePath) throws IOException {
		List<Long> skipList = new ArrayList<Long>();
		Files.readAllLines(Paths.get(filePath)).forEach(line -> {
			Long dbId = Long.parseLong(line.split("\t")[0]);
			skipList.add(dbId);
		});
		return skipList;
	}
	
	private String getLastModificationAuthor(GKInstance instance)
	{
		final String noAuthor = "No modification or creation author";
		GKInstance mostRecentMod = null;
		try
		{
			@SuppressWarnings("unchecked")
			List<GKInstance> modificationInstances = (List<GKInstance>) instance.getAttributeValuesList("modified");
			for (int index = modificationInstances.size() - 1; index > 0; index--)
			{
				GKInstance modificationInstance = modificationInstances.get(index);
				GKInstance author = (GKInstance) modificationInstance.getAttributeValue("author");
				// Skip modification instance for Solomon, Joel, or Guanming
				if (Arrays.asList("8939149", "1551959", "140537").contains(author.getDBID().toString())) 
				{
					continue;
				}
				mostRecentMod = modificationInstance;
				break;
			}
		}
		catch (InvalidAttributeException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		if (mostRecentMod == null)
		{
			GKInstance created = null;
			try
			{
				created = (GKInstance) instance.getAttributeValue("created");
			}
			catch (InvalidAttributeException e)
			{
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (created != null)
			{
				return created.getDisplayName();
			}
			else
			{
				return noAuthor;
			}
		}
		
		return mostRecentMod.getDisplayName();	
	}
	
	private Report report(MySQLAdaptor dba, String schemaClass, String attribute, String operator, List<Long> skipList)
	{
		Report r = new DelimitedTextReport();
		
		List<GKInstance> instances = new ArrayList<GKInstance>();
		instances.addAll(getInstances(this.dba, schemaClass, attribute, operator, skipList));

		for (GKInstance instance : instances)
		{
			r.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), instance.getSchemClass().getName() + " with NULL " + attribute, getLastModificationAuthor(instance)));
		}
//		List<String> reportLines = getReportLines(dba, schemaClass, attribute, operator, skipList);
		
//		if (!reportLines.isEmpty())
//		{
//			System.out.println("There are " + reportLines.size() + " " + schemaClass + " instances with a null " + attribute);
//			for (String line : reportLines) {
//				System.out.println(line);
//			}
//		} else {
//			System.out.println(schemaClass + " instances with null " + attribute + ": there are none! :)");
//		}
		//r.setColumnHeaders(Arrays.asList("DBID","DisplayName", "SchemaClass", "Issue", "MostRecentAuthor"));
		return r;
	}
	
	

	@SuppressWarnings("unchecked")
	private List<GKInstance> getInstances(MySQLAdaptor dba, String schemaClass, String attribute, String operator, List<Long> skipList)
	{
		List<GKInstance> instances = new ArrayList<GKInstance>();
		try
		{
			instances.addAll(dba.fetchInstanceByAttribute(schemaClass, attribute, operator, null));
			
			if (skipList != null && !skipList.isEmpty())
			{
				//List<GKInstance> filteredList = instances.parallelStream().filter(inst -> skipList.contains(inst.getDBID())).collect(Collectors.toList());
				return instances.parallelStream().filter(inst -> skipList.contains(inst.getDBID())).collect(Collectors.toList());
			}
			else
			{
				return instances;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return instances;
	}
	
//	private List<String> getReportLines(MySQLAdaptor currentDBA, String schemaClass, String attribute, String operator, List<Long> skipList) {
//		List<String> reportLines = new ArrayList<String>();
//		
//		List<GKInstance> instances = new ArrayList<GKInstance>();
//		instances.addAll(getInstances(currentDBA, schemaClass, attribute, operator, skipList));
//	
//		for (GKInstance instance : instances) {
//			reportLines.add(getReportLine(instance, schemaClass + " with null " + attribute));
//		}
//		
//		return reportLines;
//	}
	
	@Override
	public Report executeQACheck()
	{
		Report reactionLikeEventReport = new DelimitedTextReport();
		
		try
		{
			Report r = report(this.dba, "ReactionlikeEvent", "compartment", "IS NULL", getRLECompartmentSkipList());
			reactionLikeEventReport.addLines(r.getReportLines());
		}
		catch (IOException e)
		{
			System.err.println("Unable to get RLE compartment skip list");
			e.printStackTrace();
		}
		
		try
		{
			Report r = report(this.dba, "ReactionlikeEvent", "input", "IS NULL", getRLEInputSkipList());
			reactionLikeEventReport.addLines(r.getReportLines());
		}
		catch (IOException e)
		{
			System.err.println("Unable to get RLE input skip list");
			e.printStackTrace();
		}
		
		try
		{
			Report r = report(this.dba, "ReactionlikeEvent", "output", "IS NULL", getRLEOutputSkipList());
			reactionLikeEventReport.addLines(r.getReportLines());
		}
		catch (IOException e)
		{
			System.err.println("Unable to get RLE output skip list");
			e.printStackTrace();
		}
		reactionLikeEventReport.setColumnHeaders(Arrays.asList("DBID","DisplayName", "SchemaClass", "Issue", "MostRecentAuthor"));
		return reactionLikeEventReport;
	}

}

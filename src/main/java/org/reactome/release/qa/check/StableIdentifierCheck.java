package org.reactome.release.qa.check;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQATest;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport; 

/**
 * Need to consider a database level enforcement.
 * @author wug
 *
 */
@SuppressWarnings("unchecked")
@SliceQATest
public class StableIdentifierCheck extends AbstractQACheck {

	public StableIdentifierCheck() {
    }
	
	@Override
	public QAReport executeQACheck() throws Exception {
	    QAReport report = new QAReport();
	    report.setColumnHeaders("DB_ID", "DisplayName", "Issue", "LastAuthor");
	    
	    Collection<GKInstance> stableIds = dba.fetchInstancesByClass(ReactomeJavaConstants.StableIdentifier);
	    dba.loadInstanceAttributeValues(stableIds, new String[] {ReactomeJavaConstants.identifier});
	    Map<String, Set<GKInstance>> idToInsts = new HashMap<>();
	    for (GKInstance stableId : stableIds) {
	        String id = (String) stableId.getAttributeValue(ReactomeJavaConstants.identifier);
	        if (id == null) {
	            report.addLine(stableId.getDBID().toString(),
	                           stableId.getDisplayName(),
	                           "Empty identifier",
	                           QACheckerHelper.getLastModificationAuthor(stableId));
	            continue;
	        }
	        idToInsts.compute(id, (key, set) -> {
	            if (set == null)
	                set = new HashSet<>();
	            set.add(stableId);
	            return set;
	        });
	        // Check if this stableId is used
	        Collection<GKInstance> referrers = stableId.getReferers(ReactomeJavaConstants.stableIdentifier);
	        if (referrers == null || referrers.size() == 0) {
	            report.addLine(stableId.getDBID().toString(), 
	                    stableId.getDisplayName(), 
	                    "Not used", 
	                    QACheckerHelper.getLastModificationAuthor(stableId));
	        }
	        else if (referrers.size() > 1) {
	            report.addLine(stableId.getDBID().toString(), 
                        stableId.getDisplayName(), 
                        "Referred more than once", 
                        QACheckerHelper.getLastModificationAuthor(stableId));
	        }
	    }
	    
	    idToInsts.forEach((key, set) -> {
	        if (set.size() == 1)
	            return;
	        set.forEach(stableId -> {
	            report.addLine(stableId.getDBID().toString(), 
                        stableId.getDisplayName(), 
                        "Duplicated identifier", 
                        QACheckerHelper.getLastModificationAuthor(stableId));
	        });
	    });
	    
	    return report;
	}

    @Override
    public String getDisplayName() {
        return "StableIdentifier_Identifier";
    }

}

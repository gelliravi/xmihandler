package gov.nih.nci.ncicb.xmiinout.writer;

import gov.nih.nci.ncicb.xmiinout.domain.UMLTaggedValue;
import gov.nih.nci.ncicb.xmiinout.domain.bean.JDomDomainObject;


public interface UMLTaggedValueWriter {

	public void writeValue(UMLTaggedValue taggedValue);

}

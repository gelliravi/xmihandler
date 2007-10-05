
package gov.nih.nci.ncicb.xmiinout.handler.impl;

import gov.nih.nci.ncicb.xmiinout.domain.UMLAssociation;
import gov.nih.nci.ncicb.xmiinout.domain.UMLAssociationEnd;
import gov.nih.nci.ncicb.xmiinout.domain.UMLAttribute;
import gov.nih.nci.ncicb.xmiinout.domain.UMLClass;
import gov.nih.nci.ncicb.xmiinout.domain.UMLDatatype;
import gov.nih.nci.ncicb.xmiinout.domain.UMLDependency;
import gov.nih.nci.ncicb.xmiinout.domain.UMLDependencyEnd;
import gov.nih.nci.ncicb.xmiinout.domain.UMLGeneralization;
import gov.nih.nci.ncicb.xmiinout.domain.UMLModel;
import gov.nih.nci.ncicb.xmiinout.domain.UMLPackage;
import gov.nih.nci.ncicb.xmiinout.domain.UMLTaggedValue;
import gov.nih.nci.ncicb.xmiinout.domain.UMLVisibility;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLAssociationBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLAssociationEndBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLAttributeBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLClassBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLDependencyBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLGeneralizationBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLModelBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLPackageBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLStereotypeDefinitionBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLTagDefinitionBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLVisibilityBean;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jaxen.JaxenException;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * 
 */
public class ArgoUMLDefaultImpl extends DefaultXmiHandler {

	private static Logger logger = Logger.getLogger(ArgoUMLDefaultImpl.class.getName());


	protected Element rootElement;

	protected Map<String, UMLClassBean> idClassMap = new HashMap<String, UMLClassBean>();

	protected void _load(String filename) {

		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(filename);
			rootElement = doc.getRootElement();

			logger.debug("rootElement.getName(): " + rootElement.getName());      
			logger.debug("rootElement.getName(): " + rootElement.getName());
			readModel(rootElement);

		} catch (Exception ex) {
			throw new RuntimeException("Error initializing model", ex);
		}

	}

	protected void _load(java.net.URI uri) {

		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(uri.toURL());
			rootElement = doc.getRootElement();
			readModel(rootElement);

		} catch (Exception ex) {
			throw new RuntimeException("Error initializing model", ex);
		}

	}


	public void save(String filename) throws IOException {
		File f = new File(filename);

		Writer writer = new OutputStreamWriter
		(new FileOutputStream(f), "UTF-8");
		XMLOutputter xmlout = new XMLOutputter();
		xmlout.setFormat(Format.getPrettyFormat());
		writer.write(xmlout.outputString(rootElement));
		writer.flush();
		writer.close();

	}

	public UMLModel getModel() {
		Iterator<UMLModel> it = models.values().iterator();
		if(it.hasNext())
			return it.next();
		else 
			return null;
	}

	public UMLModel getModel(String modelName) {
		return models.get(modelName);
	}

	protected void readModel(Element rootElement) throws JaxenException {

		String xpath = "/uml/XMI/XMI.content/*[local-name()='Model']";
		Namespace ns = Namespace.getNamespace("org.omg.xmi.namespace.UML");

		JDOMXPath path = new JDOMXPath(xpath);
		List<Element> elts = path.selectNodes(rootElement);

		logger.debug("Elements Found:  " + elts.size());

		for(Element elt : elts) {
			UMLModelBean model = ArgoJDomXmiTransformer.toUMLModel(elt);
			models.put(model.getName(), model);

			logger.debug("Model Name:  " + model.getName());

			for(UMLTagDefinitionBean def : doTagDefinitions(elt, ns)) {
				ArgoJDomXmiTransformer.addTagDefinition(def);
			}

			for(UMLDatatype type : doDataTypes(elt, ns)) {
				model.addDatatype(type);
				ArgoJDomXmiTransformer.addDatatype(type);
			}
			

			for(UMLStereotypeDefinitionBean stereotype : doStereotypeDefinitions(elt, ns)) {
				ArgoJDomXmiTransformer.addStereotypeDefinition(stereotype);
			}			

			for(UMLPackage pkg : doPackages(elt, ns)) {
				model.addPackage(pkg);
			}

			for(UMLClass clazz : doClasses(elt, ns)) {
				model.addClass(clazz);
			}

			for(UMLGeneralization gen : doGeneralizations(elt, ns)) {
				model.addGeneralization(gen);
			}

			for(UMLDependency dep : doDependencies(elt, ns)) {
				model._addDependency(dep);
			}

			for(UMLAssociation assoc : doAssociations(elt, ns)) {
				model.addAssociation(assoc);
			}      
		}

		doRootTaggedValues(rootElement, ns);

		// Must be done after classes for cross references.
		ArgoJDomXmiTransformer.completeAttributes(ns);
	}


	protected List<UMLTaggedValue> doRootTaggedValues(Element rootElement, Namespace ns) 
	throws JaxenException {
		String xpath = "/XMI/XMI.content/*[local-name()='TaggedValue']";
		JDOMXPath path = new JDOMXPath(xpath);

		List<Element> elts = path.selectNodes(rootElement);

		List<UMLTaggedValue> result = new ArrayList<UMLTaggedValue>();

		if ( elts != null) {
			logger.debug("Root Tagged Values found: " + elts.size());
		} else {
			logger.debug("No Root Tagged Values found");
		}

		for(Element tvElt : elts) {
			UMLTaggedValue tv = ArgoJDomXmiTransformer.toUMLTaggedValue(tvElt, ns);

			if(tv != null)
				result.add(tv);

			Attribute refAtt = tvElt.getAttribute("modelElement");
			if(refAtt != null) {
				UMLClassBean clazz = idClassMap.get(refAtt.getValue());
				if(clazz != null) {
					clazz.addTaggedValue(tv);
				}
			}
		}

		return result;

	}

	protected List<UMLTagDefinitionBean> doTagDefinitions(Element elt, Namespace ns) {
		Element ownedElement = elt.getChild("Namespace.ownedElement", ns);	    

		if (ownedElement == null){
			logger.debug("ownedElement is null for Element " + elt.getAttributeValue("name"));
			return (List)new ArrayList();
		}

		List<UMLTagDefinitionBean> result = new ArrayList<UMLTagDefinitionBean>();

		List<Element> tdElements = (List<Element>)ownedElement.getChildren("TagDefinition", ns);
		logger.debug("TagDefinition Elements found: " + tdElements.size());

		for(Element tdElt : tdElements) {
			UMLTagDefinitionBean td = ArgoJDomXmiTransformer.toUMLTagDefinition(tdElt);
			logger.debug("TagDefinition: " + td.getName() + ", xmi.id: " + td.getXmiId());
			if(td != null)
				result.add(td);
		}

		return result;
	}

	protected List<UMLPackageBean> doPackages(Element elt, Namespace ns) {
		Element ownedElement = elt.getChild("Namespace.ownedElement", ns);

		if (ownedElement == null){
			logger.debug("ownedElement is null for Element " + elt.getAttributeValue("name"));
			return (List)new ArrayList();
		}

		List<Element> packageElements = (List<Element>)ownedElement.getChildren("Package", ns);

		logger.debug("Package size: " + packageElements.size());
		List<UMLPackageBean> result = new ArrayList<UMLPackageBean>();

		for(Element pkgElement : packageElements) {
			logger.debug("Package name: " + pkgElement.getAttributeValue("name"));
			UMLPackageBean umlPkg = ArgoJDomXmiTransformer.toUMLPackage(pkgElement);
			result.add(umlPkg);

			Collection<UMLTaggedValue> taggedValues = doTaggedValues(pkgElement, ns);
			for(UMLTaggedValue tv : taggedValues) {
				umlPkg.addTaggedValue(tv);
			}

			for(UMLPackageBean pkg : doPackages(pkgElement, ns)) {
				umlPkg.addPackage(pkg);
			}
			for(UMLClassBean clazz : doClasses(pkgElement, ns)) {
				umlPkg.addClass(clazz);
			}
		}

		return result;

	}

	protected List<UMLClassBean> doClasses(Element elt, Namespace ns) {
		Element ownedElement = elt.getChild("Namespace.ownedElement", ns);

		if (ownedElement == null){
			logger.debug("ownedElement is null for Element " + elt.getAttributeValue("name"));
			return (List)new ArrayList();
		}

		List<Element> classElements = (List<Element>)ownedElement.getChildren("Class", ns);
		List<UMLClassBean> result = new ArrayList<UMLClassBean>();

		for(Element classElement : classElements) {
			UMLClassBean umlClass = ArgoJDomXmiTransformer.toUMLClass(classElement, ns);

			Collection<UMLTaggedValue> taggedValues = doTaggedValues(classElement, ns);
			for(UMLTaggedValue tv : taggedValues) {
				umlClass.addTaggedValue(tv);
			}

			List<UMLAttribute> atts = doAttributes(classElement, ns);
			for(UMLAttribute att : atts) {
				umlClass.addAttribute(att);
			}

			idClassMap.put(umlClass.getModelId(), umlClass);
			result.add(umlClass);

		}

		return result;

	}


	protected List<UMLDatatype> doDataTypes(Element modelElt, Namespace ns) {
		Element ownedElement = modelElt.getChild("Namespace.ownedElement", ns);

		List<Element> typeElements = (List<Element>)ownedElement.getChildren("DataType", ns);

		logger.debug("DataTypeElements size: " + typeElements.size());
		logger.debug("DataTypeElements size: " + typeElements.size());

		List<UMLDatatype> result = new ArrayList<UMLDatatype>();

		for(Element typeElt : typeElements) {
			result.add(ArgoJDomXmiTransformer.toUMLDatatype(typeElt));
		}
		return result;

	}
	
	protected List<UMLStereotypeDefinitionBean> doStereotypeDefinitions(Element modelElt, Namespace ns) {
		Element ownedElement = modelElt.getChild("Namespace.ownedElement", ns);

		List<Element> typeElements = (List<Element>)ownedElement.getChildren("Stereotype", ns);

		logger.debug("Stereotype Elements size: " + typeElements.size());

		List<UMLStereotypeDefinitionBean> result = new ArrayList<UMLStereotypeDefinitionBean>();

		for(Element typeElt : typeElements) {
			result.add(ArgoJDomXmiTransformer.toUMLStereotypeDefinition(typeElt));
		}
		return result;

	}	

	protected List<UMLTaggedValue> doTaggedValues(Element elt, Namespace ns) {
		Element modelElement = elt.getChild("ModelElement.taggedValue", ns);

		List<UMLTaggedValue> result = new ArrayList<UMLTaggedValue>();
		if(modelElement == null)
			return result;

		List<Element> tvElements = (List<Element>)modelElement.getChildren("TaggedValue", ns);
		logger.debug("TaggedValue Elements found: " + tvElements.size());
		for(Element tvElt : tvElements) {
			UMLTaggedValue tv = ArgoJDomXmiTransformer.toUMLTaggedValue(tvElt, ns);
			if(tv != null)
				result.add(tv);
		}

		return result;
	}

	protected  List<UMLAttribute> doAttributes(Element classElement, Namespace ns) {
		Element featureElement = classElement.getChild("Classifier.feature", ns);

		List<UMLAttribute> result = new ArrayList<UMLAttribute>();
		if(featureElement == null)
			return result;

		List<Element> attElements = (List<Element>)featureElement.getChildren("Attribute", ns);

		for(Element attElt : attElements) {
			UMLAttributeBean umlAtt = ArgoJDomXmiTransformer.toUMLAttribute(attElt, ns);

			Collection<UMLTaggedValue> taggedValues = doTaggedValues(attElt, ns);
			for(UMLTaggedValue tv : taggedValues) {
				umlAtt.addTaggedValue(tv);
			}

			result.add(umlAtt);
		}

		return result;

	}

	protected List<UMLDependency> doDependencies(Element modelElement, Namespace ns) throws JaxenException {
		String xpath = "//*[local-name()='Dependency']";

		JDOMXPath path = new JDOMXPath(xpath);
		List<Element> depElts = path.selectNodes(rootElement);

		logger.debug("Dependency Elements Found: " + depElts.size());

		List<UMLDependency> result = new ArrayList<UMLDependency>();

		for(Element depElt : depElts) {

			String xmiId = depElt.getAttributeValue("xmi.id");
			if (xmiId == null ) { continue; }

			logger.debug("depElt.getAttributeValue('xmi.id'): " + xmiId);    	

			Element clientElement = depElt.getChild("Dependency.client", ns);
			Element clientClassElement = clientElement.getChild("Class", ns);

			logger.debug("clientClassElement: " + clientClassElement.getAttributeValue("xmi.idref")); 

			Element supplierElement = depElt.getChild("Dependency.supplier", ns);
			Element supplierClassElement = supplierElement.getChild("Class", ns);

			logger.debug("supplierClassElement: " + supplierClassElement.getAttributeValue("xmi.idref"));  

			UMLDependencyEnd client = idClassMap.get(clientClassElement.getAttributeValue("xmi.idref"));
			logger.debug("client: " + client);

			UMLDependencyEnd supplier = idClassMap.get(supplierClassElement.getAttributeValue("xmi.idref"));
			logger.debug("supplier: " + supplier);

			if(client == null) {
				logger.debug("Can't find client for dependency: " + depElt.getAttribute("xmi.id") + " -- only dependencies to classes are supported -- ignoring");
				continue;
			}
			if(supplier == null) {
				logger.debug("Can't find supplier for dependency: " + depElt.getAttribute("xmi.id") + " -- only dependencies to classes are supported -- ignoring");
				continue;
			}

			Attribute nameAtt = depElt.getAttribute("name");
			String depName = null;
			if(nameAtt != null)
				depName = nameAtt.getValue();

			Attribute visAtt = depElt.getAttribute("visibility");
			UMLVisibility visibility = null;
			if(visAtt != null) {
				visibility = new UMLVisibilityBean(visAtt.getValue());
			}

			String stereotype = null;
			List<Element> elts = (List<Element>) depElt.getChildren("ModelElement.stereotype", ns);
			if (elts.size() > 0) {
				Element modelStElt = elts.get(0);
				List<Element> stElts = (List<Element>) modelStElt.getChildren("Stereotype", ns);
				if (stElts.size() > 0) {
					stereotype = ArgoJDomXmiTransformer.getStereotypeName(stElts.get(0));
					logger.debug("Dependency Stereotype:  " + stereotype);
				}
			}

			UMLDependencyBean depBean = new UMLDependencyBean(depElt, depName, visibility, client, supplier, stereotype);

			Collection<UMLTaggedValue> taggedValues = doTaggedValues(depElt, ns);
			for(UMLTaggedValue tv : taggedValues) {
				depBean.addTaggedValue(tv);
			}

			result.add(depBean);
		}    

		return result;

	}


	protected List<UMLGeneralization> doGeneralizations(Element modelElement, Namespace ns) throws JaxenException {
		String xpath = "//*[local-name()='Generalization']";

		JDOMXPath path = new JDOMXPath(xpath);
		List<Element> genElts = path.selectNodes(rootElement);

		logger.debug("Number of Generalization Elements Found: " + genElts.size());

		List<UMLGeneralization> result = new ArrayList<UMLGeneralization>();

		if (genElts == null || genElts.isEmpty()){
			logger.debug("No Generalization Elements found");
			return (List)new ArrayList();
		}

		for(Element genElt : genElts) {

			if (genElt.getAttributeValue("xmi.id") == null ) { continue; }

//			logger.debug("genElt.getAttributeValue('xmi.id'): " + genElt.getAttributeValue("xmi.id"));

			Element childElement = genElt.getChild("Generalization.child", ns);
			Element childClassElement = childElement.getChild("Class", ns);

//			logger.debug("*** childClassElement: " + childClassElement.getAttributeValue("xmi.idref")); 

			Element parentElement = genElt.getChild("Generalization.parent", ns);
			Element parentClassElement = parentElement.getChild("Class", ns);

//			logger.debug("*** parentClassElement: " + parentClassElement.getAttributeValue("xmi.idref"));  

			UMLClassBean subClass = idClassMap.get(childClassElement.getAttributeValue("xmi.idref"));
			logger.debug("*** subClass name: " + subClass.getName());

			UMLClassBean superClass = idClassMap.get(parentClassElement.getAttributeValue("xmi.idref"));
			logger.debug("*** superClass name: " + superClass.getName());      
//			result.add(ArgoJDomXmiTransformer.toUMLGeneralization(genElt));
			result.add(new UMLGeneralizationBean(genElt, superClass, subClass));
		}    

		return result;

	}

	protected List<UMLAssociation> doAssociations(Element modelElement, Namespace ns) throws JaxenException {
		String xpath = "//*[local-name()='Association']";

		JDOMXPath path = new JDOMXPath(xpath);
		List<Element> assocElts = path.selectNodes(rootElement);

		logger.debug("Number of Association Elements Found: " + assocElts.size());    

		List<UMLAssociation> result = new ArrayList<UMLAssociation>();

		for(Element assocElt : assocElts) {

			String xmiId = assocElt.getAttributeValue("xmi.id");
			if (xmiId == null ) { continue; }

			logger.debug("assocElt.getAttributeValue('xmi.id'): " + xmiId);    	

			Element connectionElement = assocElt.getChild("Association.connection", ns);

			if(connectionElement == null)
				continue;

			List<Element> endElements = (List<Element>)connectionElement.getChildren("AssociationEnd", ns);

			logger.debug("Number of Association End Elements Found: " + endElements.size());

			UMLAssociationEndBean srcEnd = null, targetEnd = null;

			for(Element endElt : endElements) {

				Element participantElement = endElt.getChild("AssociationEnd.participant", ns);
				Element participantClassElement = participantElement.getChild("Class", ns);
				logger.debug("participantClassElement: " + participantClassElement.getAttributeValue("xmi.idref")); 

				UMLClassBean endClass = idClassMap.get(participantClassElement.getAttributeValue("xmi.idref"));
				logger.debug("AssociationEnd Class: " + endClass.getName());

//				EA
//				<UML:AssociationEnd visibility="public" multiplicity="1..*" name="organization" aggregation="none" isOrdered="false" isNavigable="true" type="EAID_C090E3E1_AFE9_48cd_B28C_130C42C6A4C7">
//				<UML:ModelElement.taggedValue/>
//				</UML:AssociationEnd>

//				ArgoUML
//				<UML:AssociationEnd.multiplicity>
//				<UML:Multiplicity xmi.id = '-64--88-1-107-c5495e:111b95291de:-8000:000000000000314A'>
//				<UML:Multiplicity.range>
//				<UML:MultiplicityRange xmi.id = '-64--88-1-107-c5495e:111b95291de:-8000:0000000000003149'
//				lower = '0' upper = '-1'/>
//				</UML:Multiplicity.range>
//				</UML:Multiplicity>
//				</UML:AssociationEnd.multiplicity>        

				// Get Multiplicity Range
				Element assocEndMultiplicity = endElt.getChild("AssociationEnd.multiplicity", ns);
				Element multiplicityElement = assocEndMultiplicity.getChild("Multiplicity", ns);
				Element multiplicityDotRangeElement = multiplicityElement.getChild("Multiplicity.range", ns);
				Element multiplicityRangeElement = multiplicityDotRangeElement.getChild("MultiplicityRange", ns);	  	
				logger.debug("multiplicityRangeElement: " + multiplicityRangeElement.getAttributeValue("xmi.id"));         

				int low = 0, high = 0;
				org.jdom.Attribute lowerMultAttr = multiplicityRangeElement.getAttribute("lower");
				org.jdom.Attribute upperMultAttr = multiplicityRangeElement.getAttribute("upper");        

				if(lowerMultAttr != null && upperMultAttr != null) {
					String lower = lowerMultAttr.getValue();
					logger.debug("multiplicity lower:  " + lower);

					String upper = upperMultAttr.getValue();
					logger.debug("multiplicity upper:  " + upper);

					low = Integer.valueOf(lower);
					high = Integer.valueOf(upper);          
				}

				boolean navigable = Boolean.valueOf(endElt.getAttribute("isNavigable").getValue());
				logger.debug("isNavigable: " + navigable);

				org.jdom.Attribute nameAtt = endElt.getAttribute("name");
				String name = nameAtt != null?nameAtt.getValue():"";
				logger.debug("name: " + name);

				UMLAssociationEndBean endBean = new UMLAssociationEndBean
				(endElt,
						endClass,
						name,
						low,
						high,
						navigable);

				if(srcEnd == null)
					srcEnd = endBean;
				else
					targetEnd = endBean;

				Collection<UMLTaggedValue> taggedValues = doTaggedValues(endElt, ns);
				for(UMLTaggedValue tv : taggedValues) {
					logger.debug("taggedValue: " + tv.getName());
					endBean.addTaggedValue(tv);
				}

			}

			List<UMLAssociationEnd> endBeans = new ArrayList<UMLAssociationEnd>();
			endBeans.add(srcEnd);
			endBeans.add(targetEnd);

			if(srcEnd.getUMLElement() == null || targetEnd.getUMLElement() == null) {
				logger.info("Can't find end class for Association: " + assocElt.getAttribute("xmi.id") + " -- only associations to classes are supported -- ignoring");
				continue;
			}


			Attribute nameAtt = assocElt.getAttribute("name");
			String assocRoleName = null;
			if(nameAtt != null)
				assocRoleName = nameAtt.getValue();


			UMLAssociationBean assoc = new UMLAssociationBean(assocElt, assocRoleName, endBeans);

			Collection<UMLTaggedValue> taggedValues = doTaggedValues(assocElt, ns);
			for(UMLTaggedValue tv : taggedValues) {
				assoc.addTaggedValue(tv);
			}

			result.add(assoc);
		}    

		return result;

	}



}
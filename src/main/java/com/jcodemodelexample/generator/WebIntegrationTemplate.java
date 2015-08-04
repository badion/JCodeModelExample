package com.jcodemodelexample.generator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import com.epam.eccl.dao.GenericDao;
import com.epam.eccl.dao.impl.GenericDaoImpl;


public class WebIntegrationTemplate {

	private static final String TEST_DS_XML = "test-ds.xml";
	
	private static final String BEANS_XML = "beans.xml";
	
	private static final String PERSISTENCE_XML = "META-INF/persistence.xml";
	
	private static final String TEST_PERSISTENCE_XML = "META-INF/test-persistence.xml";
	
	private static final String ECCL_ENUMS = "com.badion.eccl.enums";
	
	private static final String ECCL_MODEL = "com.badion.eccl.model";
	
	private static final String ECCL_UTIL= "com.badion.eccl.util";
	
	private static final String JAR = ".jar";
	
	private Class<?> clazz;
	
	private Set<Class<?>> dependencies;
	
	private Set<String> packages;

	private WebIntegrationTemplate(Class<?> clazz) {
		this.clazz = clazz;
		this.dependencies = new HashSet<Class<?>>();
		this.packages = new HashSet<String>();
	}

	public static WebIntegrationTemplate newInstance(Class<?> clazz) {
		return new WebIntegrationTemplate(clazz);
	}

	public void addDependencies(Class<?>... classes) {
		for (Class<?> clazz : classes) {
			this.dependencies.add(clazz);
		}
	}

	public void addPackages(String... packages) {
		this.packages.addAll(Arrays.asList(packages));
	}

	public JavaArchive build() {
		return ShrinkWrap.create(JavaArchive.class, clazz.getSimpleName() + JAR).addClasses(GenericDao.class, GenericDaoImpl.class)
				.addPackages(true, ECCL_MODEL, ECCL_ENUMS, ECCL_UTIL)
				.addClasses(dependencies.toArray(new Class<?>[dependencies.size()]))
				.addAsManifestResource(EmptyAsset.INSTANCE, BEANS_XML)
				.addAsResource(TEST_PERSISTENCE_XML, PERSISTENCE_XML)
				.addAsResource(TEST_DS_XML, TEST_DS_XML)
				.addPackages(true, packages.toArray(new String[packages.size()]));
				
	}
	

}

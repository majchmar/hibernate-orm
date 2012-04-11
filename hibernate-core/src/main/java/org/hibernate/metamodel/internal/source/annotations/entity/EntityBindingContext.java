/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.internal.source.annotations.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;

import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.jaxb.Origin;
import org.hibernate.internal.jaxb.SourceType;
import org.hibernate.internal.util.Value;
import org.hibernate.metamodel.internal.source.annotations.IdentifierGeneratorSourceContainer;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.IdGenerator;
import org.hibernate.metamodel.spi.domain.Type;
import org.hibernate.metamodel.spi.source.IdentifierGeneratorSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MappingDefaults;
import org.hibernate.metamodel.spi.source.MetadataImplementor;
import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContext;
import org.hibernate.service.ServiceRegistry;

/**
 * Annotation version of a local binding context.
 * 
 * @author Steve Ebersole
 */
public class EntityBindingContext implements LocalBindingContext, AnnotationBindingContext {
	private final AnnotationBindingContext contextDelegate;
	private final Origin origin;

	private final Map<String,IdGenerator> localIdentifierGeneratorDefinitionMap;

	public EntityBindingContext(AnnotationBindingContext contextDelegate, ConfiguredClass source) {
		this.contextDelegate = contextDelegate;
		this.origin = new Origin( SourceType.ANNOTATION, source.getName() );

		localIdentifierGeneratorDefinitionMap = processLocalIdentifierGeneratorDefinitions( source.getClassInfo() );
	}

	private Map<String,IdGenerator> processLocalIdentifierGeneratorDefinitions(final ClassInfo classInfo) {
		Iterable<IdentifierGeneratorSource> identifierGeneratorSources = extractIdentifierGeneratorSources(
				new IdentifierGeneratorSourceContainer() {
					@Override
					public List<AnnotationInstance> getSequenceGeneratorSources() {
						List<AnnotationInstance> generatorSources = classInfo.annotations().get( JPADotNames.SEQUENCE_GENERATOR );
						return generatorSources == null ? Collections.<AnnotationInstance>emptyList() : generatorSources;
					}

					@Override
					public List<AnnotationInstance> getTableGeneratorSources() {
						List<AnnotationInstance> generatorSources = classInfo.annotations().get( JPADotNames.TABLE_GENERATOR );
						return generatorSources == null ? Collections.<AnnotationInstance>emptyList() : generatorSources;
					}

					@Override
					public List<AnnotationInstance> getGenericGeneratorSources() {
						List<AnnotationInstance> annotations = new ArrayList<AnnotationInstance>();

						List<AnnotationInstance> generatorAnnotations = classInfo.annotations().get( HibernateDotNames.GENERIC_GENERATOR );
						if ( generatorAnnotations != null ) {
							annotations.addAll( generatorAnnotations );
						}

						List<AnnotationInstance> generatorsAnnotations = classInfo.annotations().get( HibernateDotNames.GENERIC_GENERATORS );
						if ( generatorsAnnotations != null ) {
							for ( AnnotationInstance generatorsAnnotation : generatorsAnnotations ) {
								Collections.addAll(
										annotations,
										JandexHelper.getValue( generatorsAnnotation, "value", AnnotationInstance[].class )
								);
							}
						}
						return annotations;
					}
				}
		);

		Map<String,IdGenerator> map = new HashMap<String, IdGenerator>();
		for ( IdentifierGeneratorSource source : identifierGeneratorSources ) {
			map.put(
					source.getGeneratorName(),
					new IdGenerator(
							source.getGeneratorName(),
							source.getGeneratorImplementationName(),
							source.getParameters()
					)
			);
		}
		return map;
	}

	@Override
	public Origin getOrigin() {
		return origin;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return contextDelegate.getServiceRegistry();
	}

	@Override
	public NamingStrategy getNamingStrategy() {
		return contextDelegate.getNamingStrategy();
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		return contextDelegate.getMappingDefaults();
	}

	@Override
	public MetadataImplementor getMetadataImplementor() {
		return contextDelegate.getMetadataImplementor();
	}

	@Override
	public <T> Class<T> locateClassByName(String name) {
		return contextDelegate.locateClassByName( name );
	}

	@Override
	public Type makeJavaType(String className) {
		return contextDelegate.makeJavaType( className );
	}

	@Override
	public boolean isGloballyQuotedIdentifiers() {
		return contextDelegate.isGloballyQuotedIdentifiers();
	}

	@Override
	public Value<Class<?>> makeClassReference(String className) {
		return contextDelegate.makeClassReference( className );
	}

	@Override
	public String qualifyClassName(String name) {
		return contextDelegate.qualifyClassName( name );
	}

	@Override
	public Index getIndex() {
		return contextDelegate.getIndex();
	}

	@Override
	public ClassInfo getClassInfo(String name) {
		return contextDelegate.getClassInfo( name );
	}

	@Override
	public void resolveAllTypes(String className) {
		contextDelegate.resolveAllTypes( className );
	}

	@Override
	public ResolvedType getResolvedType(Class<?> clazz) {
		return contextDelegate.getResolvedType( clazz );
	}

	@Override
	public ResolvedTypeWithMembers resolveMemberTypes(ResolvedType type) {
		return contextDelegate.resolveMemberTypes( type );
	}

	@Override
	public Iterable<IdentifierGeneratorSource> extractIdentifierGeneratorSources(IdentifierGeneratorSourceContainer container) {
		return contextDelegate.extractIdentifierGeneratorSources( container );
	}

	@Override
	public IdGenerator findIdGenerator(String name) {
		IdGenerator definition = localIdentifierGeneratorDefinitionMap.get( name );
		if ( definition == null ) {
			contextDelegate.findIdGenerator( name );
		}
		return definition;
	}
}
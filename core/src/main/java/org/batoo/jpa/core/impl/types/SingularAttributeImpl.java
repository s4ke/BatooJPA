/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.batoo.jpa.core.impl.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Deque;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;
import javax.persistence.Version;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.batoo.jpa.core.MappingException;
import org.batoo.jpa.core.impl.mapping.BasicColumnTemplate;
import org.batoo.jpa.core.impl.mapping.BasicMapping;
import org.batoo.jpa.core.impl.mapping.EmbeddedMapping;
import org.batoo.jpa.core.impl.mapping.MetamodelImpl;
import org.batoo.jpa.core.impl.mapping.OwnedOneToOneMapping;
import org.batoo.jpa.core.impl.mapping.OwnerManyToOneMapping;
import org.batoo.jpa.core.impl.mapping.OwnerOneToOneMapping;
import org.batoo.jpa.core.impl.mapping.TypeFactory;
import org.batoo.jpa.core.impl.reflect.ReflectHelper;
import org.batoo.jpa.core.jdbc.IdType;

/**
 * The implementation of {@link SingularAttribute}.
 * 
 * @author hceylan
 * @since $version
 */
public final class SingularAttributeImpl<X, T> extends AttributeImpl<X, T> implements SingularAttribute<X, T> {

	// Parse phase properties
	private boolean optional; // if the field is optional
	private boolean version; // if this is a version type attribute

	// Link phase properties
	private IdType idType;
	private String generatorName;

	/**
	 * @param declaringType
	 *            the type declaring this attribute
	 * @param member
	 *            the {@link Member} this attribute is associated with
	 * @param javaMember
	 *            the java type of the member
	 * @throws MappingException
	 *             thrown in case of a parsing error
	 * 
	 * @since $version
	 * @author hceylan
	 */
	public SingularAttributeImpl(ManagedType<X> declaringType, Member member, Class<T> javaMember) throws MappingException {
		super(declaringType, member, javaMember);
	}

	/**
	 * Fills the sequence / table generated value.
	 * 
	 * @since $version
	 * @author hceylan
	 */
	public void fillValue(Object instance) {
		if (!this.isId()) {
			throw new IllegalStateException("Not an id attribute");
		}

		switch (this.idType) {
			case SEQUENCE:
				this.set(instance, this.declaringType.getMetaModel().getNextSequence(this.generatorName));
				break;
			case TABLE:
				this.set(instance, this.declaringType.getMetaModel().getNextTableValue(this.generatorName));
				break;
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public final Class<T> getBindableJavaType() {
		return this.javaType;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public final BindableType getBindableType() {
		return BindableType.SINGULAR_ATTRIBUTE;
	}

	/**
	 * Returns the generatorName.
	 * 
	 * @return the generatorName
	 * @since $version
	 */
	public String getGeneratorName() {
		return this.generatorName;
	}

	/**
	 * Returns the idType.
	 * 
	 * @return the idType
	 * @since $version
	 */
	public IdType getIdType() {
		return this.idType;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public final PersistentAttributeType getPersistentAttributeType() {
		switch (this.attributeType) {
			case ASSOCIATION:
				return this.many ? PersistentAttributeType.MANY_TO_ONE : PersistentAttributeType.ONE_TO_ONE;
			case BASIC:
				return PersistentAttributeType.BASIC;
			case EMBEDDED:
				return PersistentAttributeType.EMBEDDED;
			default: // LOB
				return PersistentAttributeType.BASIC;
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public TypeImpl<T> getType() {
		return (TypeImpl<T>) this.getDeclaringType().getMetaModel().getType(this.javaType);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public final boolean isCollection() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public boolean isId() {
		return this.idType != null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public final boolean isOptional() {
		return this.optional;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public final boolean isVersion() {
		return this.version;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public final void link(Deque<AttributeImpl<?, ?>> path) throws MappingException {
		switch (this.attributeType) {
			case LOB:
			case BASIC:
				this.mapping = new BasicMapping<X, T>(this, path, this.columns);
				break;
			case EMBEDDED:
				this.mapping = new EmbeddedMapping<X, T>(this, path);
				break;
			case ASSOCIATION:
				final boolean eager = this.fetchType == FetchType.EAGER;
				if (!this.many || StringUtils.isNotBlank(this.mappedBy)) {
					this.mapping = new OwnedOneToOneMapping<X, T>(this, path, this.orphanRemoval, eager);
				}
				else if (!this.many) {
					this.mapping = new OwnerOneToOneMapping<X, T>(this, path, this.columns, eager);
				}
				else {
					this.mapping = new OwnerManyToOneMapping<X, T>(this, path, this.columns, eager);
				}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	protected Set<Class<? extends Annotation>> parse() throws MappingException {
		final Set<Class<? extends Annotation>> parsed = super.parse();

		final Member member = this.getJavaMember();

		this.parseId(parsed, member);

		this.version = ReflectHelper.getAnnotation(member, Version.class) != null;

		final Embedded embedded = ReflectHelper.getAnnotation(member, Embedded.class);
		if (embedded != null) {
			this.attributeType = AtrributeType.EMBEDDED;

			parsed.add(Embedded.class);
		}

		final Basic basic = ReflectHelper.getAnnotation(member, Basic.class);
		if (basic != null) {
			this.optional = basic.optional();
			this.fetchType = basic.fetch();

			parsed.add(Basic.class);
		}

		final Column column = ReflectHelper.getAnnotation(this.javaMember, Column.class);
		if (column != null) {
			if (StringUtils.isNotBlank(column.columnDefinition())) {
				throw new NotImplementedException("Column.columnDefinition() is not supported: "
					+ ReflectHelper.createMemberName(this.javaMember));
			}

			ReflectHelper.validateColumnParameters(this.javaMember, column.precision(), column.scale(), column.length());

			if ((column.length() <= 0)) {
				throw new MappingException("Length must be positive: " + ReflectHelper.createMemberName(this.javaMember));
			}

			this.columns.add(new BasicColumnTemplate<X, T>(this, this.idType, this.generatorName, column));

			parsed.add(Column.class);
		}

		final ManyToOne manyToOne = ReflectHelper.getAnnotation(member, ManyToOne.class);
		if (manyToOne != null) {
			this.attributeType = AtrributeType.ASSOCIATION;
			this.many = true;

			this.optional = manyToOne.optional();
			this.fetchType = manyToOne.fetch();
			this.cascadeType = manyToOne.cascade();

			parsed.add(ManyToOne.class);
		}

		final OneToOne oneToOne = ReflectHelper.getAnnotation(member, OneToOne.class);
		if (oneToOne != null) {
			this.attributeType = AtrributeType.ASSOCIATION;

			this.mappedBy = oneToOne.mappedBy();
			this.optional = oneToOne.optional();
			this.fetchType = oneToOne.fetch();
			this.cascadeType = oneToOne.cascade();
			this.orphanRemoval = oneToOne.orphanRemoval();

			parsed.add(OneToOne.class);
		}

		return parsed;
	}

	private void parseId(final Set<Class<? extends Annotation>> parsed, final Member member) throws MappingException {
		final MetamodelImpl metaModel = this.declaringType.getMetaModel();

		final boolean id = ReflectHelper.getAnnotation(member, Id.class) != null;
		if (id) {
			// Check if the type is identifiable
			if (!(this.declaringType instanceof IdentifiableTypeImpl)) {
				throw new MappingException("Id can only be specified on Entities and MapperSuperclasses. Specified on "
					+ ReflectHelper.createMemberName(member));
			}

			final IdentifiableTypeImpl<X> identifiableType = (IdentifiableTypeImpl<X>) this.declaringType;

			final GeneratedValue generatedValue = ReflectHelper.getAnnotation(member, GeneratedValue.class);
			if (generatedValue != null) {
				this.generatorName = generatedValue.generator();
				this.idType = TypeFactory.getIdType(generatedValue.strategy());

				parsed.add(GeneratedValue.class);
			}

			this.parseSequenceGenerator(parsed, member, metaModel, identifiableType, generatedValue);

			this.parseTableGenerator(parsed, member, metaModel, identifiableType, generatedValue);

			this.supportsIdType(metaModel, identifiableType, member);

			parsed.add(Id.class);
		}
	}

	private void parseSequenceGenerator(final Set<Class<? extends Annotation>> parsed, final Member member, final MetamodelImpl metaModel,
		final IdentifiableTypeImpl<X> identifiableType, GeneratedValue generatedValue) throws MappingException {
		final SequenceGenerator sequenceGenerator = ReflectHelper.getAnnotation(member, SequenceGenerator.class);
		if (sequenceGenerator != null) {
			if (identifiableType.getSequenceGenerator() != null) {
				throw new MappingException("SequenceGenerator declared on both entity and attribute "
					+ ReflectHelper.createMemberName(member));
			}

			if (identifiableType.getTableGenerator() != null) {
				throw new MappingException("TableGenerator and SequenceGenerator declared on both entity and attribute "
					+ ReflectHelper.createMemberName(member));
			}

			// is the name ok?
			if (StringUtils.isBlank(sequenceGenerator.name())) {
				throw new MappingException("SequenceGenerator name must be specified, defined on " + ReflectHelper.createMemberName(member));
			}

			// is it unique
			if (!metaModel.addSequenceGenerator(sequenceGenerator)) {
				throw new MappingException("An existing sequence generator with name " + sequenceGenerator.name()
					+ " already refefined on " + ReflectHelper.createMemberName(member));
			}

			this.idType = IdType.SEQUENCE;
			this.generatorName = sequenceGenerator.name();

			parsed.add(SequenceGenerator.class);
		}

		this.validateSequenceGenerator(parsed, member, metaModel, identifiableType, sequenceGenerator, generatedValue);
	}

	private void parseTableGenerator(final Set<Class<? extends Annotation>> parsed, final Member member, final MetamodelImpl metaModel,
		final IdentifiableTypeImpl<X> identifiableType, GeneratedValue generatedValue) throws MappingException {
		final TableGenerator tableGenerator = ReflectHelper.getAnnotation(member, TableGenerator.class);
		if (tableGenerator != null) {
			if (identifiableType.getTableGenerator() != null) {
				throw new MappingException("Table generator declared on both entity and attribute "
					+ ReflectHelper.createMemberName(member));
			}

			if (identifiableType.getSequenceGenerator() != null) {
				throw new MappingException("SequenceGenerator and TableGenerator declared on both entity and attribute "
					+ ReflectHelper.createMemberName(member));
			}

			// is the name ok?
			if (StringUtils.isBlank(tableGenerator.name())) {
				throw new MappingException("Tableenerator name must be specified, defined on " + ReflectHelper.createMemberName(member));
			}

			// is it unique
			if (!metaModel.addTableGenerator(tableGenerator)) {
				throw new MappingException("An existing sequence generator with name " + tableGenerator.name() + " already refefined on "
					+ ReflectHelper.createMemberName(member));
			}

			this.idType = IdType.TABLE;
			this.generatorName = tableGenerator.name();

			parsed.add(TableGenerator.class);
		}

		this.validateTableGenerator(parsed, member, metaModel, identifiableType, tableGenerator, generatedValue);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void set(Object instance, Object value) {
		this.getAccessor().set(instance, (T) value);
	}

	private void supportsIdType(MetamodelImpl metaModel, IdentifiableTypeImpl<X> identifiableType, Member member) throws MappingException {
		final IdType supported = metaModel.getJdbcAdapter().supports(this.idType);

		if (supported == null) {
			throw new MappingException("Database adapter does not support generation type " + this.idType + ", specified on "
				+ ReflectHelper.createMemberName(member));
		}

		this.idType = supported;

		if ((this.idType == IdType.SEQUENCE) && StringUtils.isBlank(this.generatorName)) {
			if (identifiableType.getSequenceGenerator() != null) {
				this.generatorName = identifiableType.getSequenceGenerator().name();
			}
			else {
				metaModel.addSequenceGenerator(metaModel.getDefaultSequenceGenerator());
			}
		}

		if ((this.idType == IdType.TABLE) && StringUtils.isBlank(this.generatorName)) {
			if (identifiableType.getSequenceGenerator() != null) {
				this.generatorName = identifiableType.getTableGenerator().name();
			}
			else {
				metaModel.addTableGenerator(metaModel.getDefaultTableIdGenerator());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public final String toString() {
		return "SingularAttributeImpl [name=" + this.getName() + ", owner=" + this.declaringType.getName() + ", id=" + this.idType
			+ ", many=" + this.many + ", optional=" + this.optional + ", type=" + this.getType().getName() + ", version=" + this.version
			+ "]";
	}

	private void validateSequenceGenerator(Set<Class<? extends Annotation>> parsed, Member member, MetamodelImpl metaModel,
		IdentifiableTypeImpl<X> identifiableType, SequenceGenerator sequenceGenerator, GeneratedValue generatedValue)
		throws MappingException {
		if (sequenceGenerator == null) {
			return;
		}

		// only allow AUTO, SEQUENCE and TABLE if SequenceGenerator specified
		if ((generatedValue.strategy() != GenerationType.AUTO) && (generatedValue.strategy() != GenerationType.SEQUENCE)) {
			throw new MappingException("Conflicting GeneratedValue strategy with SequenceGenerator: " + generatedValue.strategy()
				+ " specified on " + ReflectHelper.createMemberName(member));
		}

		// SequenceGenerator name and GeneratedValue generator must be in line.
		if (StringUtils.isNotBlank(generatedValue.generator()) && !sequenceGenerator.name().equals(generatedValue.generator())) {
			throw new MappingException("Conflicting SequenceGenerator.name GeneratedValue.generator values : " + sequenceGenerator.name()
				+ " - " + generatedValue.generator() + " specified on " + ReflectHelper.createMemberName(member));
		}

	}

	private void validateTableGenerator(Set<Class<? extends Annotation>> parsed, Member member, MetamodelImpl metaModel,
		IdentifiableTypeImpl<X> identifiableType, TableGenerator tableGenerator, GeneratedValue generatedValue) throws MappingException {
		if (tableGenerator == null) {
			return;
		}

		// only allow AUTO, SEQUENCE and TABLE if SequenceGenerator specified
		if (tableGenerator != null) {
			if ((generatedValue.strategy() != GenerationType.AUTO) && (generatedValue.strategy() != GenerationType.SEQUENCE)) {
				throw new MappingException("Conflicting GeneratedValue strategy with TableGenerator: " + generatedValue.strategy()
					+ " specified on " + ReflectHelper.createMemberName(member));
			}

			// SequenceGenerator name and GeneratedValue generator must be in line.
			if (StringUtils.isNotBlank(generatedValue.generator()) && !tableGenerator.name().equals(generatedValue.generator())) {
				throw new MappingException("Conflicting SequenceGenerator.name GeneratedValue.generator values : " + tableGenerator.name()
					+ " - " + generatedValue.generator() + " specified on " + ReflectHelper.createMemberName(member));
			}

		}
	}
}
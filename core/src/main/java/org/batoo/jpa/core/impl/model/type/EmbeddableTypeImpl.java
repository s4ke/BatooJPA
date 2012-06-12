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
package org.batoo.jpa.core.impl.model.type;

import javax.persistence.metamodel.EmbeddableType;

import org.batoo.jpa.core.impl.model.MetamodelImpl;
import org.batoo.jpa.parser.metadata.type.EmbeddableMetadata;

/**
 * Implementation of {@link EmbeddableType}.
 * 
 * @param <X>
 *            The represented embeddable type
 * 
 * @author hceylan
 * @since $version
 */
public class EmbeddableTypeImpl<X> extends ManagedTypeImpl<X> implements EmbeddableType<X> {

	/**
	 * @param metamodel
	 *            the metamodel
	 * @param javaType
	 *            the java type
	 * @param metadata
	 *            the metadata
	 * 
	 * @since $version
	 * @author hceylan
	 */
	public EmbeddableTypeImpl(MetamodelImpl metamodel, Class<X> javaType, EmbeddableMetadata metadata) {
		super(metamodel, javaType, metadata);

		this.addAttributes(metadata);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}
}

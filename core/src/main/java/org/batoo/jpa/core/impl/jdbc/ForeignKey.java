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
package org.batoo.jpa.core.impl.jdbc;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * Foreign key definition.
 * 
 * @author hceylan
 * @since $version
 */
public class ForeignKey {

	private final String tableName;
	private final String referenceTableName;
	private final List<PhysicalColumn> columns;
	private final String name;

	/**
	 * @param tableName
	 *            the name of the table
	 * @param name
	 *            the name of the foreign key
	 * @param columns
	 *            the column
	 * 
	 * @since $version
	 * @author hceylan
	 */
	public ForeignKey(String tableName, String referenceTableName, List<PhysicalColumn> columns) {
		super();

		this.tableName = tableName;
		this.columns = columns;
		this.referenceTableName = referenceTableName;

		this.name = this.generateName();
	}

	/**
	 * @return
	 * 
	 * @since $version
	 * @author hceylan
	 */
	private String generateName() {
		final String columnNames = Joiner.on("_").join(Lists.transform(this.columns, new Function<PhysicalColumn, String>() {

			@Override
			public String apply(PhysicalColumn input) {
				return input.getReferencedColumn().getPhysicalName();
			}
		}));

		return this.referenceTableName + "_" + columnNames;
	}

	/**
	 * Returns the columns.
	 * 
	 * @return the columns
	 * @since $version
	 */
	public List<PhysicalColumn> getColumns() {
		return this.columns;
	}

	/**
	 * Returns the name.
	 * 
	 * @return the name
	 * @since $version
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the referenceTableName.
	 * 
	 * @return the referenceTableName
	 * @since $version
	 */
	public String getReferenceTableName() {
		return this.referenceTableName;
	}

	/**
	 * Returns the tableName.
	 * 
	 * @return the tableName
	 * @since $version
	 */
	public String getTableName() {
		return this.tableName;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public String toString() {
		return "ForeignKey [tableName=" + this.tableName + ", name=" + this.name + ", referenceTableName=" + this.referenceTableName
			+ ", columns=" + this.columns + "]";
	}

}

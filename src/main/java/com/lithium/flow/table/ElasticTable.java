/*
 * Copyright 2015 Lithium Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lithium.flow.table;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import com.lithium.flow.util.Unchecked;

import javax.annotation.Nonnull;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;

/**
 * @author Matt Ayres
 */
public class ElasticTable implements Table {
	private final Client client;
	private final String index;
	private final String type;

	public ElasticTable(@Nonnull Client client, @Nonnull String index, @Nonnull String type) {
		this.client = checkNotNull(client);
		this.index = checkNotNull(index);
		this.type = checkNotNull(type);
	}

	@Override
	@Nonnull
	public Row getRow(@Nonnull Key key) {
		SearchHits hits = client.prepareSearch(index).setTypes(type)
				.setQuery(QueryBuilders.termQuery("_id", key.id()))
				.execute().actionGet().getHits();

		Row row = new Row(key);
		if (hits.totalHits() > 0) {
			row.putAll(hits.getAt(0).getSource());
		}
		return row;
	}

	@Override
	public void putRow(@Nonnull Row row) {
		Key key = row.getKey();
		Row newRow = new Row(key).putAll(getRow(key)).putAll(row);

		Unchecked.run(() -> {
			XContentBuilder content = jsonBuilder().startObject();
			for (String column : newRow.columns()) {
				content.field(column, newRow.getCell(column, Object.class));
			}
			content = content.endObject();

			client.prepareIndex(index, type, key.id()).setSource(content).execute().actionGet();
		});
	}

	@Override
	public void deleteRow(@Nonnull Key key) {
		client.prepareDelete(index, type, key.id()).execute().actionGet();
	}
}

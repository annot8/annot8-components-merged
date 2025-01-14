/* Annot8 (annot8.io) - Licensed under Apache-2.0. */
package io.annot8.components.mongo.utils;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public interface MongoConnection<D> {

  MongoDatabase getDatabase();

  default <T> MongoCollection<T> getCollection(Class<T> clazz) {
    return getCollection().withDocumentClass(clazz);
  }

  MongoCollection<D> getCollection();

  void disconnect();
}

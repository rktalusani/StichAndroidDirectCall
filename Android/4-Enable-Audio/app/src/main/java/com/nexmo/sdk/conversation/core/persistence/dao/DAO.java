package com.nexmo.sdk.conversation.core.persistence.dao;

import java.util.Collection;

/**
 * Data Access Objects interface for CRUD operations.
 * DAOs abstract access to the database in a clean way.
 *
 * @author emma tresanszki.
 */
interface DAO<T> {

    //CRUD
    void insert(T dto, String id);
    void update(T dto, String id);
    boolean delete(String id);
    boolean delete(Collection<String> cIds);
}

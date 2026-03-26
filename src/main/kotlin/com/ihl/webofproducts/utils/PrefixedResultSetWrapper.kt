package com.ihl.webofproducts.utils

import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Wrapper for ResultSet that strips a prefix from column names.
 * This allows reusing row mappers that expect unprefixed column names.
 */
class PrefixedResultSetWrapper(
    private val delegate: ResultSet,
    private val prefix: String
) : ResultSet by delegate {

    override fun getObject(columnLabel: String): Any? {
        return delegate.getObject(prefix + columnLabel)
    }

//    override fun getObject(columnLabel: String, type: Class<*>): Any? {
//        return delegate.getObject(prefix + columnLabel, type)
//    }

    override fun getString(columnLabel: String): String? {
        return delegate.getString(prefix + columnLabel)
    }

    override fun getLong(columnLabel: String): Long {
        return delegate.getLong(prefix + columnLabel)
    }

    override fun getDouble(columnLabel: String): Double {
        return delegate.getDouble(prefix + columnLabel)
    }

    override fun getTimestamp(columnLabel: String): Timestamp? {
        return delegate.getTimestamp(prefix + columnLabel)
    }

    override fun getInt(columnLabel: String): Int {
        return delegate.getInt(prefix + columnLabel)
    }
}
package org.sqlite.driver;

import org.sqlite.ErrCodes;
import org.sqlite.SQLiteException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.SQLXML;

public class SQLXMLFromRows implements SQLXML {
	private /*final*/ Rows rs;
	private final int columnIndex;

	private boolean freed;
	private boolean readable;

	public SQLXMLFromRows(Rows rs, int columnIndex) {
		this.rs = rs;
		this.columnIndex = columnIndex;
		this.readable = true;
	}

	@Override
	public void free() {
		freed = true;
		rs = null;
	}

	@Override
	public InputStream getBinaryStream() throws SQLException {
		checkAndSwitchReadable();
		return rs.getBinaryStream(columnIndex);
	}
	@Override
	public OutputStream setBinaryStream() throws SQLException {
		checkNotFreed();
		throw new SQLException("This SQLXML object is not writable.");
	}

	@Override
	public Reader getCharacterStream() throws SQLException {
		checkAndSwitchReadable();
		return getReader();
	}
	@Override
	public Writer setCharacterStream() throws SQLException {
		checkNotFreed();
		throw new SQLException("This SQLXML object is not writable.");
	}

	@Override
	public String getString() throws SQLException {
		checkAndSwitchReadable();
		return rs.getString(columnIndex);
	}
	@Override
	public void setString(String value) throws SQLException {
		checkNotFreed();
		throw new SQLException("This SQLXML object is not writable.");
	}

	@Override
	public <T extends Source> T getSource(Class<T> sourceClass) throws SQLException {
		checkAndSwitchReadable();
		if (sourceClass == null || DOMSource.class.equals(sourceClass)) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			try {
				DocumentBuilder builder = factory.newDocumentBuilder();
				// TODO builder.setErrorHandler();
				InputSource input = new InputSource(getReader());
				return (T) new DOMSource(builder.parse(input));
			} catch (ParserConfigurationException | SAXException | IOException e) {
				throw new SQLiteException(null, "Unable to decode xml data.", ErrCodes.WRAPPER_SPECIFIC, e);
			}
		} else if (SAXSource.class.equals(sourceClass)) {
			InputSource is = new InputSource(getReader());
			return (T) new SAXSource(is);
		} else if (StreamSource.class.equals(sourceClass)) {
			return (T) new StreamSource(getReader());
		} else if (StAXSource.class.equals(sourceClass)) {
			XMLInputFactory xif = XMLInputFactory.newInstance();
			try {
				XMLStreamReader xsr = xif.createXMLStreamReader(getReader());
				return (T) new StAXSource(xsr);
			} catch (XMLStreamException e) {
				throw new SQLiteException(null, "Unable to decode xml data.", ErrCodes.WRAPPER_SPECIFIC, e);
			}
		}
		throw new SQLiteException("Unknown XML Source class: " + sourceClass, ErrCodes.WRAPPER_SPECIFIC);
	}

	@Override
	public <T extends Result> T setResult(Class<T> resultClass) throws SQLException {
		checkNotFreed();
		throw new SQLException("This SQLXML object is not writable.");
	}

	private Reader getReader() throws SQLException {
		return rs.getCharacterStream(columnIndex);
	}

	private void checkNotFreed() throws SQLException {
		if (freed) {
			throw new SQLException("This SQLXML object has already been freed.");
		}
	}
	private void checkAndSwitchReadable() throws SQLException {
		checkNotFreed();
		if (!readable) {
			throw new SQLException("This SQLXML object has already been read.");
		}
		readable = false;
	}
}

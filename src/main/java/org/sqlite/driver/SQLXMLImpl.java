package org.sqlite.driver;

import org.sqlite.ErrCodes;
import org.sqlite.SQLiteException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.sql.SQLXML;

public class SQLXMLImpl implements SQLXML {
	private final Charset charset;
	private Src src;
	private boolean freed;
	private boolean writable;

	public SQLXMLImpl(Charset charset) {
		this.charset = charset;
		this.writable = true;
	}

	@Override
	public void free() {
		freed = true;
		src = null;
	}

	@Override
	public InputStream getBinaryStream() throws SQLException {
		checkAndSwitchReadable();
		return src.getBinaryStream();
	}
	@Override
	public OutputStream setBinaryStream() throws SQLException { // no streaming...
		checkAndSwitchWritable();
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); // FileBackedOutputStream
		src = new OutputStreamSrc(outputStream);
		return outputStream;
	}

	@Override
	public Reader getCharacterStream() throws SQLException {
		checkAndSwitchReadable();
		return src.getCharacterStream();
	}
	@Override
	public Writer setCharacterStream() throws SQLException { // no streaming...
		checkAndSwitchWritable();
		return createWriter();
	}

	@Override
	public String getString() throws SQLException {
		checkAndSwitchReadable();
		return src.getString();
	}
	@Override
	public void setString(String value) throws SQLException {
		checkAndSwitchWritable();
		if (value == null) {
			src = NULL;
		} else {
			src = new StringSrc(value);
		}
	}

	@Override
	public <T extends Source> T getSource(Class<T> sourceClass) throws SQLException {
		checkAndSwitchReadable();
		if (src == NULL) {
			return null;
		}
		if (sourceClass == null || DOMSource.class.equals(sourceClass)) {
			return (T) src.getDOMSource();
		} else if (SAXSource.class.equals(sourceClass)) {
			return sourceClass.cast(new SAXSource(getInputSource()));
		} else if (StreamSource.class.equals(sourceClass)) {
			if (src.isBinary()) {
				return sourceClass.cast(new StreamSource(src.getBinaryStream()));
			} else {
				return sourceClass.cast(new StreamSource(src.getCharacterStream()));
			}
		} else if (StAXSource.class.equals(sourceClass)) {
			XMLInputFactory xif = XMLInputFactory.newInstance();
			try {
				final XMLStreamReader xsr;
				if (src.isBinary()) {
					xsr = xif.createXMLStreamReader(src.getBinaryStream());
				} else {
					xsr = xif.createXMLStreamReader(src.getCharacterStream());
				}
				return sourceClass.cast(new StAXSource(xsr));
			} catch (XMLStreamException e) {
				throw new SQLiteException(null, "Unable to decode xml data.", ErrCodes.WRAPPER_SPECIFIC, e);
			}
		}
		throw new SQLiteException("Unknown XML Source class: " + sourceClass, ErrCodes.WRAPPER_SPECIFIC);
	}

	private InputSource getInputSource() throws SQLException {
		if (src.isBinary()) {
			return new InputSource(src.getBinaryStream());
		} else {
			return new InputSource(src.getCharacterStream());
		}
	}

	@Override
	public <T extends Result> T setResult(Class<T> resultClass) throws SQLException {
		checkAndSwitchWritable();
		if (resultClass == null || DOMResult.class.equals(resultClass)) {
			final DOMResult domResult = new DOMResult();
			src = new DOMSrc(domResult);
			return (T) domResult;
		} else if (SAXResult.class.equals(resultClass)) {
			try {
				SAXTransformerFactory transformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
				TransformerHandler transformerHandler = transformerFactory.newTransformerHandler();
				transformerHandler.setResult(new StreamResult(createWriter()));
				return resultClass.cast(new SAXResult(transformerHandler));
			} catch (TransformerException e) {
				throw new SQLiteException(null, "Unable to create SAXResult.", ErrCodes.WRAPPER_SPECIFIC, e);
			}
		} else if (StreamResult.class.equals(resultClass)) {
			return resultClass.cast(new StreamResult(createWriter()));
		} else if (StAXResult.class.equals(resultClass)) {
			try {
				XMLOutputFactory xof = XMLOutputFactory.newInstance();
				XMLStreamWriter xsw = xof.createXMLStreamWriter(createWriter());
				return resultClass.cast(new StAXResult(xsw));
			} catch (XMLStreamException e) {
				throw new SQLiteException(null, "Unable to create StAXResult.", ErrCodes.WRAPPER_SPECIFIC, e);
			}
		}
		throw new SQLiteException("Unknown XML Result class: " + resultClass, ErrCodes.WRAPPER_SPECIFIC);
	}

	private Writer createWriter() {
		StringWriter writer = new StringWriter();
		src = new WriterSrc(writer);
		return writer;
	}

	private void checkNotFreed() throws SQLException {
		if (freed) {
			throw new SQLException("This SQLXML object has already been freed.");
		}
	}
	private void checkAndSwitchReadable() throws SQLException {
		checkNotFreed();
		if (src == null) {
			throw new SQLException("This SQLXML object has not been written.");
		}
		writable = false;
	}
	private void checkAndSwitchWritable() throws SQLException {
		checkNotFreed();
		if (!writable) {
			throw new SQLException("This SQLXML object has already been written.");
		}
		writable = false;
	}

	private interface Src {
		Reader getCharacterStream() throws SQLException;
		String getString() throws SQLException;
		InputStream getBinaryStream() throws SQLException;
		boolean isBinary();
		DOMSource getDOMSource() throws SQLException;
	}

	private static final Src NULL = new Src() {
		@Override
		public Reader getCharacterStream() {
			return null;
		}
		@Override
		public String getString() {
			return null;
		}
		@Override
		public InputStream getBinaryStream() {
			return null;
		}
		@Override
		public boolean isBinary() {
			return false;
		}
		@Override
		public DOMSource getDOMSource() {
			return null;
		}
	};

	private final class StringSrc implements Src {
		private final String value;

		private StringSrc(String value) {
			this.value = value;
		}

		@Override
		public Reader getCharacterStream() {
			return new StringReader(value);
		}
		@Override
		public String getString() {
			return value;
		}
		@Override
		public InputStream getBinaryStream() {
			return new ByteArrayInputStream(value.getBytes(charset));
		}
		@Override
		public boolean isBinary() {
			return false;
		}
		@Override
		public DOMSource getDOMSource() throws SQLException {
			return SQLXMLImpl.getDOMSource(new InputSource(getCharacterStream()));
		}
	}

	private final class OutputStreamSrc implements Src {
		private final ByteArrayOutputStream outputStream;

		private OutputStreamSrc(ByteArrayOutputStream outputStream) {
			this.outputStream = outputStream;
		}

		@Override
		public Reader getCharacterStream() {
			return new StringReader(getString());
		}
		@Override
		public String getString() {
			return outputStream.toString(charset);
		}
		@Override
		public InputStream getBinaryStream() {
			return new ByteArrayInputStream(outputStream.toByteArray());
		}
		@Override
		public boolean isBinary() {
			return true;
		}
		@Override
		public DOMSource getDOMSource() throws SQLException {
			return SQLXMLImpl.getDOMSource(new InputSource(getBinaryStream()));
		}
	}

	private final class WriterSrc implements Src {
		private final StringWriter writer;

		private WriterSrc(StringWriter writer) {
			this.writer = writer;
		}

		@Override
		public Reader getCharacterStream() {
			return new StringReader(getString());
		}
		@Override
		public String getString() {
			return writer.toString();
		}
		@Override
		public InputStream getBinaryStream() {
			return new ByteArrayInputStream(getString().getBytes(charset));
		}
		@Override
		public boolean isBinary() {
			return false;
		}
		@Override
		public DOMSource getDOMSource() throws SQLException {
			return SQLXMLImpl.getDOMSource(new InputSource(getCharacterStream()));
		}
	}

	private final class DOMSrc implements Src {
		private final DOMResult result;

		private DOMSrc(DOMResult result) {
			this.result = result;
		}

		@Override
		public Reader getCharacterStream() throws SQLException {
			return new StringReader(getString());
		}
		@Override
		public String getString() throws SQLException {
			StringWriter writer = new StringWriter();
			transform(getDOMSource(), new StreamResult(writer));
			return writer.toString();
		}

		@Override
		public InputStream getBinaryStream() throws SQLException {
			return new ByteArrayInputStream(getString().getBytes(charset));
		}
		@Override
		public boolean isBinary() {
			return false;
		}
		@Override
		public DOMSource getDOMSource() {
			return new DOMSource(result.getNode());
		}
	}

	private static void transform(Source source, StreamResult target) throws SQLException {
		TransformerFactory factory = TransformerFactory.newInstance();
		try {
			Transformer transformer = factory.newTransformer();
			transformer.transform(source, target);
		} catch (TransformerException e) {
			throw new SQLiteException(null, "Unable to decode xml data.", ErrCodes.WRAPPER_SPECIFIC, e);
		}
	}

	private static DOMSource getDOMSource(InputSource source) throws SQLException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			// TODO builder.setErrorHandler();
			return new DOMSource(builder.parse(source));
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new SQLiteException(null, "Unable to decode xml data.", ErrCodes.WRAPPER_SPECIFIC, e);
		}
	}
}

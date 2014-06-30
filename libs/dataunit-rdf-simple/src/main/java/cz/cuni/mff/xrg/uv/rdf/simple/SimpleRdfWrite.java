package cz.cuni.mff.xrg.uv.rdf.simple;

import cz.cuni.mff.xrg.odcs.commons.dpu.DPUContext;
import cz.cuni.mff.xrg.odcs.rdf.WritableRDFDataUnit;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add write functionality to {@link SimpleRdfRead} by wrapping
 * {@link WritableRDFDataUnit}.
 *
 * @author Škoda Petr
 */
public class SimpleRdfWrite extends SimpleRdfRead {

	private static final Logger LOG = LoggerFactory.getLogger(
			SimpleRdfWrite.class);

	/**
	 * Add policy.
	 */
	protected AddPolicy addPolicy = AddPolicy.IMMEDIATE;

	/**
	 * Max size of {@link #toAddBuffer}. If the buffer is larger and
	 * {@link #add(org.openrdf.model.Resource, org.openrdf.model.URI, org.openrdf.model.Value)}
	 * is called then the buffer is flushed by {@link #flushBuffer()}.
	 */
	protected int toAddBufferFlushSize = 100000;

	/**
	 * Buffer for triples that should be added into wrapped {@link #dataUnit}.
	 */
	protected final ArrayList<Statement> toAddBuffer = new ArrayList<>();

	/**
	 * Wrapped {@link WritableRDFDataUnit}.
	 */
	protected final WritableRDFDataUnit writableDataUnit;

	/**
	 * 
	 * @param dataUnit
	 * @param context 
	 */
	public SimpleRdfWrite(WritableRDFDataUnit dataUnit, DPUContext context) {
		super(dataUnit, context);
		this.writableDataUnit = dataUnit;
	}

	/**
	 * Add triple into repository. Based on current {@link AddPolicy} can add 
	 * triple in immediate or lazy way.
	 * 
	 * In the second case the {@link #flushBuffer()} method must be called in 
	 * order to add triples into used repository, until that the triples are
	 * stored in inner buffer - the triples are not visible in any read function.
	 * 
	 * @param s
	 * @param p
	 * @param o
	 * @throws OperationFailedException 
	 */
	public void add(Resource s, URI p, Value o) throws OperationFailedException {
		final Statement statement = getValueFactory().createStatement(s, p, o);
		// add to bufer
		toAddBuffer.add(statement);
		// based on policy
		switch (addPolicy) {
			case BUFFERED:
				// flush only if we have enough data
				if (toAddBuffer.size() > toAddBufferFlushSize) {
					LOG.trace("Flush on full buffer, size {}.",
							toAddBuffer.size());
					flushBuffer();
				}
				break;
			case IMMEDIATE:
				// flush in evry case
				flushBuffer();
				break;
		}
	}

	/**
	 * Set policy that determines how the
	 * {@link #add(org.openrdf.model.Resource, org.openrdf.model.URI, org.openrdf.model.Value)}
	 * behaves.
	 *
	 * @param policy Add policy.
	 */
	public void setPolicy(AddPolicy policy) {
		this.addPolicy = policy;
	}

	/**
	 * Immediately store buffered triples into repository. The inner buffer is
	 * cleared only if all the triples are added successfully. If throws
	 * exception then the state of repository is undefined.
	 *
	 * If
	 * {@link #add(org.openrdf.model.Resource, org.openrdf.model.URI, org.openrdf.model.Value)}
	 * is called with {@link AddPolicy#BUFFERED} this method must be called in
	 * order to save added statements into repository.
	 *
	 * @throws OperationFailedException
	 */
	public void flushBuffer() throws OperationFailedException {
		if (toAddBuffer.isEmpty()) {
			// nothing to add
			return;
		}
		try (ClosableConnection conn = new ClosableConnection(dataUnit)) {
			conn.c().begin();
			// add to repository
			conn.c().add(toAddBuffer, writableDataUnit.getWriteContext());
			conn.c().commit();
			// clear buffer
			toAddBuffer.clear();
		} catch (RepositoryException ex) {
			throw new OperationFailedException(
					"Failed to add triples into repository.", ex);
		}
	}

	/**
	 * If the extracted data are inconsistent then the extraction fail.
	 * 
	 * @param file
	 * @param format
	 * @param defaultURI
	 * @throws OperationFailedException
	 * @deprecated DPUs that use this method should rather directly add data
	 * into repository of use file data unit as output and then use FileToRdf
	 * DPU convertor
	 */
	@Deprecated
	public void extract(File file, RDFFormat format, String defaultURI) throws OperationFailedException {
		if (defaultURI == null) {
			defaultURI = file.toURI().toString();
		}

		try (ClosableConnection conn = new ClosableConnection(dataUnit)) {
			// add all in a single transaction
			LOG.trace("Extraction begins from: {}", file.toString());
			conn.c().begin();
			conn.c().add(file, defaultURI, format, writableDataUnit
					.getWriteContext());
			conn.c().commit();
			LOG.trace("Extraction done");
		} catch (RepositoryException ex) {
			throw new OperationFailedException(
					"Extraction failed.", ex);
		} catch (IOException | RDFParseException ex) {
			throw new OperationFailedException(
					"Extraction failed.", ex);
		} catch (RuntimeException ex) {
			throw new OperationFailedException(
					"Extraction failed for RuntimeException.", ex);
		}
	}

}

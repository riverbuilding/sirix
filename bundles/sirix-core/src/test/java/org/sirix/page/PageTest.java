/**
 * 
 */
package org.sirix.page;

import static org.testng.AssertJUnit.assertTrue;

import java.util.Arrays;

import org.sirix.Holder;
import org.sirix.TestHelper;
import org.sirix.api.PageReadTrx;
import org.sirix.exception.SirixException;
import org.sirix.exception.SirixIOException;
import org.sirix.io.bytepipe.ByteHandler;
import org.sirix.node.Kind;
import org.sirix.node.interfaces.Record;
import org.sirix.page.interfaces.Page;
import org.sirix.settings.Constants;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

/**
 * Test class for all classes implementing the {@link Page} interface.
 * 
 * @author Sebastian Graf, University of Konstanz
 * @auhtor Johannes Lichtenberger, University of Konstanz
 * 
 */
public class PageTest {

	/** {@link Holder} instance. */
	private Holder mHolder;

	/** Sirix {@link PageReadTrx} instance. */
	private PageReadTrx mPageReadTrx;

	@BeforeClass
	public void setUp() throws SirixException {
		TestHelper.closeEverything();
		TestHelper.deleteEverything();
		TestHelper.createTestDocument();
		mHolder = Holder.generateDeweyIDSession();
		mPageReadTrx = mHolder.getSession().beginPageReadTrx();
	}

	@AfterClass
	public void tearDown() throws SirixException {
		mPageReadTrx.close();
		mHolder.close();
	}
	
	/**
	 * Test method for {@link org.Page.page.IPage#IPage(long)} and
	 * {@link org.Page.page.IPage#getByteRepresentation()}.
	 * 
	 * @param clazz
	 *          page as class
	 * @param handlers
	 *          different pages
	 */
	@Test(dataProvider = "instantiatePages")
	public void testByteRepresentation(final Class<Page> clazz,
			final Page[] handlers) {
		for (final Page handler : handlers) {
			final ByteArrayDataOutput output = ByteStreams.newDataOutput();
			handler.serialize(output);
			final byte[] pageBytes = output.toByteArray();
			final ByteArrayDataInput input = ByteStreams.newDataInput(pageBytes);

			final ByteArrayDataOutput serializedOutput = ByteStreams.newDataOutput();
			final Page serializedPage = PageKind.getKind(handler.getClass())
					.deserializePage(input, mPageReadTrx);
			serializedPage.serialize(serializedOutput);
			assertTrue(new StringBuilder("Check for ").append(handler.getClass())
					.append(" failed.").toString(),
					Arrays.equals(pageBytes, serializedOutput.toByteArray()));
		}
	}

	/**
	 * Providing different implementations of the {@link Page} as Dataprovider to
	 * the test class.
	 * 
	 * @return different classes of the {@link ByteHandler}
	 * @throws SirixIOException
	 *           if an I/O error occurs
	 */
	@DataProvider(name = "instantiatePages")
	public Object[][] instantiatePages() throws SirixIOException {
		// IndirectPage setup.
		final IndirectPage indirectPage = new IndirectPage();
		// RevisionRootPage setup.
		// final RevisionRootPage revRootPage = new RevisionRootPage();

		// NodePage setup.
		final UnorderedKeyValuePage nodePage = new UnorderedKeyValuePage(
				TestHelper.random.nextInt(Integer.MAX_VALUE), PageKind.RECORDPAGE, Optional.<PageReference> absent(), mPageReadTrx);
		for (int i = 0; i < Constants.NDP_NODE_COUNT - 1; i++) {
			final Record record = TestHelper.generateOne();
			nodePage.setEntry(record.getNodeKey(), record);
		}
		// NamePage setup.
		final NamePage namePage = new NamePage();
		namePage.setName(TestHelper.random.nextInt(),
				new String(TestHelper.generateRandomBytes(256)), Kind.ELEMENT);

		// ValuePage setup.
		final PathPage valuePage = new PathPage();

		// PathSummaryPage setup.
		final PathSummaryPage pathSummaryPage = new PathSummaryPage();

		Object[][] returnVal = { { Page.class,
				new Page[] { indirectPage, namePage, valuePage, pathSummaryPage } } };
		return returnVal;
	}
}

/* --
COMP4321 Lab2 Exercise
Student Name:
Student ID:
Section:
Email:
*/
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.beans.StringBean;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.beans.LinkBean;
import org.htmlparser.visitors.HtmlPage;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;


class UrlData {
	private String url;
	private String pageTitle;
	private LocalDateTime Date;
	private Integer size;
	private Map<String, Integer> word_list;
	private Vector<String> child_link;

	UrlData(String url, String pageTitle, LocalDateTime Date, Integer size, Map<String, Integer> word_list, Vector<String> child_link) {
		this.url = url;
		this.pageTitle = pageTitle;
		this.Date = Date;
		this.size = size;
		this.word_list = word_list;
		this.child_link = child_link;
	}

	public String getUrl() {
		return this.url;
	}

	public String getPageTitle() { return this.pageTitle; }

	public Map<String, Integer> getWord_list() {
		return this.word_list;
	}

	public LocalDateTime getDate() {
		return this.Date;
	}

	public Integer getSize() {
		return this.size;
	}

	public Vector<String> getChildLink() {
		return this.child_link;
	}

	public void print() {
		System.out.println("Url: " + this.getUrl() + "\nLastModificaiton Date: " + this.getDate() + "\nSize: " + this.getSize() + "\nWord List: " + this.getWord_list() + "\nChild Link List: " + this.getChildLink());
	}
}

class Crawler {
	private String url;

	Crawler(String _url) {
		url = _url;
	}

	public Map<String, Integer> extractWords() throws ParserException {
		Vector<String> tempResult = new Vector<String>();
		Map<String, Integer> result = new HashMap<String, Integer>();

		StringBean bean = new StringBean();
		bean.setURL(url);
		bean.setLinks(false);
		String contents = bean.getStrings();
		StringTokenizer st = new StringTokenizer(contents);

		while (st.hasMoreTokens()) {
			tempResult.add(st.nextToken());
		}

		for(int i = 0; i < tempResult.size(); i++) {
			if(result.get(tempResult.get(i)) == null) {
				result.put(tempResult.get(i), 1);
			}
			else {
				Integer currentValue = result.get(tempResult.get(i));
				currentValue += 1;
				result.put(tempResult.get(i), currentValue);
			}
		}
		return result;
	}

	public Vector<String> extractLinks() throws ParserException {
		Vector<String> result = new Vector<String>();
		LinkBean bean = new LinkBean();
		bean.setURL(url);
		URL[] urls = bean.getLinks();
		for (URL s : urls) {
			result.add(s.toString());
		}
		return result;
	}

	public static LocalDateTime getDate(URL url) throws Exception {
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod("HEAD");
		LocalDateTime date = null;

		Map<String, List<String>> header = connection.getHeaderFields();
		if(header.get("Last-Modified") != null && (header.get("Last-Modified").size() != 0)) {
			String lastModifiedDate = header.get("Last-Modified").get(0);
			date = LocalDateTime.parse(lastModifiedDate, DateTimeFormatter.RFC_1123_DATE_TIME);
		}
		else {
			String lastModifiedDate = header.get("Date").get(0);
			if(lastModifiedDate == null || lastModifiedDate.length() == 0) {
				date = null;
			}
			date = LocalDateTime.parse(lastModifiedDate, DateTimeFormatter.RFC_1123_DATE_TIME);
		}
		connection.disconnect();
		return date;
	}

	public Integer getSize() throws Exception {
		StringBean bean = new StringBean();
		bean.setURL(url);
		bean.setLinks(false);
		String contents = bean.getStrings();
		return contents.length();
	}

	public String getTitle(String url) throws ParserException {
		Parser parser = new Parser();
		parser.setResource(url);
		TagNode tnode = new TagNode();
		tnode.setChildren(parser.extractAllNodesThatMatch(new AndFilter()));
		for (Node n: tnode.getChildren().toNodeArray()) {
			if (n instanceof TitleTag) {
				return ((TitleTag)n).getTitle();
			}
		}
		return null;
	}

	public void crawl() {
		System.out.println("Crawling: " + url);
		try {
			String title = this.getTitle(url);
			System.out.println("Title: " + title);

			LocalDateTime date = this.getDate(new URL(url));
			System.out.println("Date: " + date);

			Integer size = this.getSize();

			Map<String, Integer> words = this.extractWords();
//			CrawlerApp.mapList.add(new UrlData(url, date, size, words));

			Vector<String> links = this.extractLinks();
			for(int i = 0; i < links.size(); i++) {
				if(CrawlerApp.urlList.contains(links.get(i)) == false)
					CrawlerApp.urlList.add(links.get(i));
			}

			CrawlerApp.mapList.add(new UrlData(url, title, date, size, words, links));

		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

public class CrawlerApp {

	static public Integer MAX_NUM_PAGES = 29;
	static public Vector<UrlData> mapList;
	static public Vector<String> urlList;

	private RocksDB db;
	private Options options;

	CrawlerApp(String dbPath) throws RocksDBException {
		CrawlerApp.mapList = new Vector<UrlData>();
		CrawlerApp.urlList = new Vector<String>();
		this.options = new Options();
		this.options.setCreateIfMissing(true);
		// creat and open the database
		this.db = RocksDB.open(options, dbPath);
	}

	public void printAll() throws RocksDBException {
		RocksIterator iter = db.newIterator();

		for(iter.seekToFirst(); iter.isValid(); iter.next()) {
			System.out.println(new String(iter.key()) + "=" + new String(iter.value()));
		}
	}

	public static void main (String[] args) {

		try {
			String dbPath = "/home/ernest/Documents/COMP4321/lab2_new/db";

			CrawlerApp crawlerApp = new CrawlerApp(dbPath);

			System.out.println("Number of pages to be crawled: " + (CrawlerApp.MAX_NUM_PAGES + 1));

			Crawler initCrawler = new Crawler(args[0]);

			initCrawler.crawl();

			Integer counter = 0;

			while (CrawlerApp.urlList.size() > 0 && counter < CrawlerApp.MAX_NUM_PAGES) {
				String url = CrawlerApp.urlList.remove(0);
				Crawler crawler = new Crawler(url);
				crawler.crawl();
				counter++;
			}

			System.out.println("Size of list: " + CrawlerApp.mapList.size());

			for (int i = 0; i < CrawlerApp.mapList.size(); i++) {
				CrawlerApp.mapList.get(i).print();
			}

			System.out.println("End of crawling");

			for(int i = 0; i < mapList.size(); i++) {
				crawlerApp.db.put(mapList.get(i).getUrl().getBytes(), ("Doc"+new Integer(i).toString()).getBytes());
				StringBuilder data = new StringBuilder();
				data.append("#PageTitle#: " + mapList.get(i).getPageTitle());
				data.append("#LastModificationDate#: " + mapList.get(i).getDate().toString());
				data.append("#Size#: " + mapList.get(i).getSize());
				data.append("#WordList#: " + mapList.get(i).getWord_list().toString());
				data.append("#ChildLinkList#: " + mapList.get(i).getChildLink().toString());
				System.out.println(data.toString());
				crawlerApp.db.put(("Doc"+new Integer(i).toString()).getBytes(), data.toString().getBytes());
			}
			System.out.println("added entry");
			crawlerApp.printAll();
		}
		catch (Exception e) {
			e.printStackTrace ();
		}
	}
}
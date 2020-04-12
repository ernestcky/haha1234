/* --
COMP4321 Lab2 Exercise
Student Name:
Student ID:
Section:
Email:
*/
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.htmlparser.beans.StringBean;
import org.htmlparser.util.ParserException;
import org.htmlparser.beans.LinkBean;





class UrlData {
	private String url;
	private LocalDateTime Date;
	private Map<String, Integer> word_list;

	UrlData(String url, LocalDateTime Date, Map<String, Integer> word_list) {
		this.url = url;
		this.Date = Date;
		this.word_list = word_list;
	}

	public String getUrl() {
		return this.url;
	}

	public Map<String, Integer> getWord_list() {
		return this.word_list;
	}

	public LocalDateTime getDate() {
		return this.Date;
	}

	public void print() {
		System.out.println("Url: " + this.getUrl() + "\nLastModificaiton Date: " + this.getDate() + "\nWord List: " + this.getWord_list());
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
		// extract links in url and return them
		// ADD YOUR CODES HERE
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

		return date;
	}

	public int getSize() throws Exception {
		StringBean bean = new StringBean();
		bean.setURL(url);
		bean.setLinks(false);
		String contents = bean.getStrings();
		return contents.length();
	}

	public void crawl() {
		System.out.println("Crawling: " + url);
		try {
			LocalDateTime date = this.getDate(new URL(url));
			System.out.println("Date: " + date);
			Map<String, Integer> words = this.extractWords();
			CrawlerApp.mapList.add(new UrlData(url, date, words));

			Vector<String> links = this.extractLinks();
			for(int i = 0; i < links.size(); i++) {
				CrawlerApp.urlList.add(links.get(i));
			}
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

	public static void main (String[] args) {
		try {
			System.out.println("Number of pages to be crawled: " + (CrawlerApp.MAX_NUM_PAGES + 1));

			CrawlerApp.mapList = new Vector<UrlData>();
			CrawlerApp.urlList = new Vector<String>();
			Crawler initCrawler = new Crawler(args[0]);

//			System.out.println("Crawling: " + args[0]);
//			LocalDateTime date = Crawler.getDate(new URL(args[0]));
//			System.out.println("Date: " + date);
			initCrawler.crawl();

//			Map<String, Integer> words = initCrawler.extractWords();

//			CrawlerApp.mapList.add(new UrlData(args[0], date, words));
//			CrawlerApp.urlList = initCrawler.extractLinks();

			Integer counter = 0;

			while (CrawlerApp.urlList.size() > 0 && counter < CrawlerApp.MAX_NUM_PAGES ) {
				String url = CrawlerApp.urlList.remove(0);
//				System.out.println("Crawling: " + url);
				Crawler crawler = new Crawler(url);
				crawler.crawl();
				counter++;
			}

			System.out.println("Size of list: " + CrawlerApp.mapList.size());

			for (int i = 0; i < CrawlerApp.mapList.size(); i++) {
				CrawlerApp.mapList.get(i).print();
			}
		}
		catch (Exception e) {
			e.printStackTrace ();
		}
	}
}
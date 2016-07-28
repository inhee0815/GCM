package gcmserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.LoggerProvider;
import net.htmlparser.jericho.Source;

public class GCMServerSide {
	private static final String SSU_URL = "http://ssu.ac.kr/web/kor/plaza_d_01?p_p_id=EXT_MIRRORBBS&p_p_lifecycle=0&p_p_state=normal&p_p_mode=view&p_p_col_id=column-1&p_p_col_pos=1&p_p_col_count=2&_EXT_MIRRORBBS_struts_action=%2Fext%2Fmirrorbbs%2Fview";
	public static ArrayList<LinkedHashMap<String, String>> list1; 

	//안드로이드 푸시 메세지 설정
	public void sendMessage(String title, String url) throws IOException {// sendMessage

		Sender sender = new Sender("AIzaSyDJ8VF1dgdoBClqso8xacxnuccY9MVoG2E");// API KEY
		String regID = "fDG3Lu0LIBg:APA91bH6tfUUOL-wYygIvvNylC31maEihIxHYEod5mSK-h4Tz2mwCW330CiW2uo3fCYsbORfNTWaEAza4weuLxPY-LUfx22UIZuErhi76aEk5EeYXGqVfi7i1Q7JwR0sFm0fxmymNeqF";
		//등록ID

		//푸시 알림 표시 어플이름 -> 게시물 제목
		Message message = new Message.Builder()
				.addData("title", URLEncoder.encode("Check It Out", "utf-8").replace("+", " ")).addData("msg", title)
				.addData("link", url).build();

		try {
			Result result = sender.send(message, regID, 5); 
			System.out.println("result : " + result);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	//5분 주기로 검사
	private static void checkBoard() {
		Timer timer = new Timer();
		TimerTask m_task = new TimerTask() {
			int count = 0;

			@Override
			public void run() {
				try {
					doExportData(count++);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		};
		timer.schedule(m_task, 0, 300000);
	}

	//데이터 크롤링
	@SuppressWarnings("unchecked")
	private static void doExportData(int idx) throws Exception {
		GCMServerSide s = new GCMServerSide();
		Source source = null;
		String title = null;
		String url = null;
		
		ArrayList<LinkedHashMap<String, String>> list2 = new ArrayList<LinkedHashMap<String, String>>(); //업데이트 후 리스트

		try {
			InputStream is = new URL(SSU_URL).openStream();
			source = new Source(new InputStreamReader(is, "utf-8"));
			source.fullSequentialParse();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//테이블 찾기
		List<Element> tableList =  source.getAllElements(HTMLElementName.TABLE);
		
		for(Iterator<Element> tableIter=tableList.iterator(); tableIter.hasNext();){ 
			Element table = (Element)tableIter.next();
			String tag = table.getAttributeValue("class");
			//태그가 bbs-list 인 것을 찾아 학사 관련 게시물을 list에 넣음
			if(tag.equals("bbs-list")) {
				Element tbody = (Element) table.getAllElements(HTMLElementName.TBODY).get(0);
				int tr_count = tbody.getAllElements(HTMLElementName.TR).size();
				
				for (int i = 0; i < tr_count; i++) {
					try {
						Element tr = (Element) tbody.getAllElements(HTMLElementName.TR).get(i);
						Element td = (Element) tr.getAllElements(HTMLElementName.TD).get(1);
						Element a = (Element) td.getAllElements(HTMLElementName.A).get(0);
						if ((a.getContent().toString().substring(2, 4)).equals("학사")) {
							LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
							map.put("title", a.getContent().toString());
							map.put("url", a.getAttributeValue("href"));
							if (idx == 0) {
								list1.add(map);
							} else {
								list2.add(map);
							}
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		//전의 리스트와 업데이트된 리스트에서 차례대로 비교해서 새로운 글이 있으면 메세지 보냄
		if (idx > 0) {
			for(int i=0;i<list2.size();i++){
				LinkedHashMap<String, String> map1 = (LinkedHashMap<String, String>) list1.get(0); 
				LinkedHashMap<String, String> map2 = (LinkedHashMap<String, String>) list2.get(i);
				if(map2.get("title").equals(map1.get("title"))) {
					//System.out.println(map2.get("title") + " = " + map1.get("title"));
					break;
				} else if(!map2.get("title").equals(map1.get("title"))) {
					title = map2.get("title");
					url = map2.get("url");
					s.sendMessage(title, url);
				} else {
					System.out.println("doExportData error");
				}
				
			}
			list1 = (ArrayList<LinkedHashMap<String, String>>) list2.clone();
		}	
	}

	public static void main(String[] args) throws Exception {
		Config.LoggerProvider = LoggerProvider.DISABLED;
		list1 = new ArrayList<LinkedHashMap<String, String>>(); //업데이트 전 리스트
		checkBoard();

	}

}

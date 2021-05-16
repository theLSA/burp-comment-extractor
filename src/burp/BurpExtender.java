package burp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.xml.ws.AsyncHandler;

public class BurpExtender implements IBurpExtender, ITab, IScannerCheck, IExtensionStateListener, IHttpRequestResponse, IMessageEditorTabFactory{

	public PrintWriter stdout;
	public PrintWriter stderr;
	
    public IExtensionHelpers hps;
    public IBurpExtenderCallbacks cbs;
    

    public JPanel mainJPanel;

    
    private JTextArea htmlCommentTextArea;
    private JTextArea jsCommentTextArea;
    private JScrollPane htmlCommentScrollPane;
    private JScrollPane jsCommentScrollPane;
    
    private JButton cleanHtmlCommentButton;
    private JButton cleanJsCommentButton;
    
    
    String htmlCommentMatch;
    String htmlCommentRegex = "<!--(.*?)-->";
    
    
    String jsCommentMatch;
    String jsCommentRegex = "(?<!:)\\/\\/.*";
    
    String jsCommentMatch1;
    String jsCommentRegex1 = "\\/\\*(\\s|.)*?\\*\\/";
    
    String response = "";
    
    String[] filterSuffix = new String[]{"gif","png","bmp","jpeg","jpg","mp3","wma","flv","mp4","wmv","ogg","avi","doc","docx","xls","xlsx","ppt","pptx","txt","pdf","zip","exe","tat","ico","css","swf","apk","m3u8","ts","svg","ttf","eot","woff","woff2"};
    
    private static IMessageEditorTab markCommentTab;
    
    ArrayList<String> urlList = new ArrayList<>();
    
	@Override
	public void extensionUnloaded() {
		// TODO Auto-generated method stub
		stdout.println("burp-comment-extractor unloaded!");
		return;
		
	}
	
	
	public boolean filterStaticSuffix(String url) {
		
		int urlLen = url.split("/").length;
		
		String lastUrlStr = url.split("/")[urlLen-1];
		String urlSuffix = lastUrlStr.substring(lastUrlStr.lastIndexOf(".") + 1);
		//stdout.println("suffix:"+urlSuffix+"\n");
		
		for(int i=0;i<filterSuffix.length;i++) {
			
			if(Arrays.asList(filterSuffix).contains(urlSuffix)) {
				return false;
			}
		}
		
		return true;
	}
	
	public boolean filterResponseStatusCode(int responseStatusCode) {
		if(responseStatusCode == 200) {
			return true;
		}
		return false;
	}
	
	
	public boolean duplicateUrl(String url) {
		if(urlList.contains(url)) {
			return true;
		}
		else {
			urlList.add(url);
			return false;
		}
	}
	
	
	
	public boolean getHtmlJsComment(String response) {
		
		htmlCommentMatch = "";
		
		jsCommentMatch = "";
		
		jsCommentMatch1 = "";
		
		stdout.println("enter getHtmlJsComment function");
		
		//stdout.println("response:"+response+"\n");

		try {
			
		     
			Pattern htmlPattern = Pattern.compile("<!--(.*?)-->");
			Matcher matcher = htmlPattern.matcher(response);
		 

			while (matcher.find()) {
				
				htmlCommentMatch += matcher.group()+"\n";
			}
			
			stdout.println("\n"+"htmlCommentMatch:"+htmlCommentMatch+"\n");

		 
			
			Pattern jsPattern = Pattern.compile(jsCommentRegex);
			Matcher jsMatcher = jsPattern.matcher(response);

			//stdout.println("response:"+response+"\n");

			while (jsMatcher.find()) {
				if(jsMatcher.group().endsWith("EN\">") || jsMatcher.group().endsWith("CN\">")) {
					continue;
				}
				
				jsCommentMatch += jsMatcher.group()+"\n";
			}
			
			stdout.println("jscommentmatch:"+jsCommentMatch+"\n");
			
			
			Pattern jsPattern1 = Pattern.compile(jsCommentRegex1);
			Matcher jsMatcher1 = jsPattern1.matcher(response);
			
			while (jsMatcher1.find()) {
				if(jsMatcher1.group().endsWith("EN\">") || jsMatcher1.group().endsWith("CN\">")) {
					continue;
				}
				
				jsCommentMatch1 += jsMatcher1.group()+"\n";
			}
			
			stdout.println("jscommentmatch1:"+jsCommentMatch1+"\n");
			
			if(htmlCommentMatch == "" && jsCommentMatch == "" && jsCommentMatch1 == "") {
				return false;
			}
			
					 
		}catch(Exception ex) {
		     stdout.println("cause gethtmljscomment exception!!!");
		}

		return true;
	}
	
	
	public void printfHtmlJsComment(String url) {
		
		if(duplicateUrl(url)) {
			return;
		}
		
		if(htmlCommentMatch != "") {
			SwingUtilities.invokeLater(() -> {
				
				htmlCommentTextArea.append("-----------------------------------\n" + "URL:"+url+ " \n -----------------------------------------------------\n"+htmlCommentMatch+"\n");

			});
		}
		
		if(jsCommentMatch != "") {
			
		
	 
			SwingUtilities.invokeLater(() -> {

				jsCommentTextArea.append("-----------------------------------\n" + "URL:"+url+ " \n -----------------------------------------------------\n"+jsCommentMatch+"\n");

			});
		}
		
		if(jsCommentMatch1 != "") {
			
			
			 
			SwingUtilities.invokeLater(() -> {

				jsCommentTextArea.append("-----------------------------------\n" + "URL:"+url+ " \n -----------------------------------------------------\n"+jsCommentMatch1+"\n");

			});
		}			

	}
	

	@Override
	public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
		// TODO Auto-generated method stub
		
		callbacks.setExtensionName("burp-comment-extractor");

        this.hps = callbacks.getHelpers();
        this.cbs = callbacks;
        
        this.stdout = new PrintWriter(callbacks.getStdout(), true);
        this.stderr = new PrintWriter(callbacks.getStderr(), true);
        

        this.stdout.println("BCE(burp-comment-extractor) loaded!");
        this.stdout.println("Author:LSA");
        this.stdout.println("https://github.com/theLSA/burp-comment-extractor");
        
        callbacks.registerScannerCheck(this);
        callbacks.registerExtensionStateListener(this);
        callbacks.registerMessageEditorTabFactory(this);
        //callbacks.registerHttpListener(this);
        
        this.initUI();
        
        cbs.customizeUiComponent(mainJPanel);
        
        cbs.addSuiteTab(BurpExtender.this);
        
        
        cleanHtmlCommentButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				htmlCommentTextArea.setText("");
			}
		});
       
       
       cleanJsCommentButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				jsCommentTextArea.setText("");
			}
		});
        
		
	}
	
	public void initUI() {
		
		//fuck you layout!!!!!!!!
		mainJPanel = new JPanel();
		
		htmlCommentTextArea = new JTextArea();
		jsCommentTextArea = new JTextArea();
		
		htmlCommentScrollPane = new JScrollPane(htmlCommentTextArea);
		jsCommentScrollPane = new JScrollPane(jsCommentTextArea);
		
		cleanHtmlCommentButton = new JButton("cleanHtmlComment");
		cleanJsCommentButton = new JButton("cleanJsComment");
		
		
		mainJPanel.setLayout(null);
		
		mainJPanel.add(htmlCommentScrollPane);
		mainJPanel.add(jsCommentScrollPane);
		
		mainJPanel.add(cleanHtmlCommentButton);
		mainJPanel.add(cleanJsCommentButton);
		
		htmlCommentScrollPane.setBounds(20, 20, 400, 600);
		jsCommentScrollPane.setBounds(450, 20, 400, 600);
		
		cleanHtmlCommentButton.setBounds(20, 650, 180, 30);
		cleanJsCommentButton.setBounds(450, 650, 150, 30);
       
	}
	
	@Override
	public String getTabCaption() {
		// TODO Auto-generated method stub
		return "BCE";
	}

	@Override
	public Component getUiComponent() {
		// TODO Auto-generated method stub
		return mainJPanel;
	}
	
	
	@Override
	public List<IScanIssue> doPassiveScan(IHttpRequestResponse baseRequestResponse) {
		// TODO Auto-generated method stub
		//return null;
		
		
		byte[] baseResponse = baseRequestResponse.getResponse();
		//int offset = hps.analyzeResponse(response).getBodyOffset();
		String url = hps.analyzeRequest(baseRequestResponse).getUrl().toString();
		//byte[] content = baseRequestResponse.getResponse();
		int responseStatusCode = hps.analyzeResponse(baseResponse).getStatusCode();
		
		if(!filterStaticSuffix(url) || !(filterResponseStatusCode(responseStatusCode))) {
			return null;
		}
		
		response = new String(baseRequestResponse.getResponse());
		
		IRequestInfo ri = hps.analyzeRequest(baseRequestResponse.getHttpService(),baseRequestResponse.getRequest());
		
		stdout.println("enter doPassiveScan");
		
		Boolean htmlJsResult = getHtmlJsComment(response);
		//Boolean htmlResult = getHtmlComment(url,response);
		
		//Boolean jsResult = getJsComment(url, response);
		
		//stdout.println("JsResult:"+jsResult+"\n");
		
		if (htmlJsResult){
			printfHtmlJsComment(url);
			
			return Collections.singletonList((IScanIssue)new HtmlJsCommentIssue(baseRequestResponse,ri.getUrl()));
		}

		else {
			stdout.println(url + " - No html/js comment found." + "\n");
			return null;
		}
	}


	@Override
	public int consolidateDuplicateIssues(IScanIssue existingIssue, IScanIssue newIssue) {
		// TODO Auto-generated method stub
		return -1;
	}

	
	public class HtmlJsCommentIssue implements IScanIssue{
		
		

		private final IHttpRequestResponse[] httpMessages;
		private final URL url;
		

		private static final String ISSUE_NAME = "Found html/js comment";
		private static final String ISSUE_DETAIL = "Found html/js comment,may contain sensitive information, such as api/username/password etc";

		private static final String REMEDIATION = "Comment should not have sensitive information";

		private static final String BACKGROUND = "Comment is good,but should not have sensitive information";

		public HtmlJsCommentIssue(IHttpRequestResponse baseRequestResponse,URL url) {
			this.httpMessages = new IHttpRequestResponse[] { baseRequestResponse };
			this.url = url;
			
		}

		@Override public String getIssueDetail() {
			return ISSUE_DETAIL;
		}

		@Override public String getConfidence() { return "Firm"; }
		@Override public IHttpRequestResponse[] getHttpMessages() { return httpMessages; }
		@Override public IHttpService getHttpService() { return httpMessages[0].getHttpService(); }
		@Override public String getIssueBackground() { return BACKGROUND; }
		@Override public String getIssueName() { return ISSUE_NAME; }
		@Override public int getIssueType() { return 0; }
		@Override public String getRemediationBackground() { return null; }
		@Override public String getRemediationDetail() { return REMEDIATION; }
		@Override public String getSeverity() { return "Information"; }
		@Override public URL getUrl() { return url; }
	}

		
	

	@Override
	public byte[] getRequest() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRequest(byte[] message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public byte[] getResponse() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setResponse(byte[] message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getComment() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setComment(String comment) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getHighlight() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setHighlight(String color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IHttpService getHttpService() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setHttpService(IHttpService httpService) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<IScanIssue> doActiveScan(IHttpRequestResponse baseRequestResponse,
			IScannerInsertionPoint insertionPoint) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IMessageEditorTab createNewInstance(IMessageEditorController controller, boolean editable) {
		markCommentTab = new MarkCommentTab(controller, editable);
		return markCommentTab;
	}
	
	
	class MarkCommentTab implements IMessageEditorTab {
		private ITextEditor markInfoText;
		private byte[] currentMessage;
		private final IMessageEditorController controller;
		
		Boolean htmlJsResult1;
		Boolean htmlJsResult2;
		
		String htmlCommentMatchString;
		
		String jsCommentMatchString;
		
		String jsCommentMatchString1;

		
		public MarkCommentTab(IMessageEditorController controller, boolean editable) {
			this.controller = controller;
			markInfoText = cbs.createTextEditor();
			markInfoText.setEditable(editable);
		}
	
		@Override
		public String getTabCaption() {
			return "bceTab";
		}
	
		@Override
		public Component getUiComponent() {
			return markInfoText.getComponent();
		}
	
		@Override
		public boolean isEnabled(byte[] content, boolean isRequest) {
			
			stdout.println("enter isenable.\n");
			
			htmlJsResult1 = getHtmlJsComment(new String(content));
			
			//stdout.println("isenable response:"+response);
			
			stdout.println("isenable:\n"+htmlCommentMatch+"\n"+jsCommentMatch+"\n"+jsCommentMatch1+"\n");
			
			if (isRequest || (htmlCommentMatch == "" && jsCommentMatch == "" && jsCommentMatch1 == "")) {
				
				return false;
			}
			
			
			return true;
		}
	
		@Override
		public byte[] getMessage() {
			return currentMessage;
		}
	
		@Override
		public boolean isModified() {
			return markInfoText.isTextModified();
			//return false;
		}
	
		@Override
		public byte[] getSelectedData() {
			return markInfoText.getSelectedText();
		}
		
		public void setMessage(byte[] content, boolean isRequest) {
			
			htmlJsResult2 = getHtmlJsComment(new String(content));
			
			htmlCommentMatchString = "";
			
			jsCommentMatchString = "";
			
			jsCommentMatchString1 = "";
			
			stdout.println("enter setmessage.\n");
			
			stdout.println("setmessage:\n"+htmlCommentMatch+"\n"+jsCommentMatch+"\n"+jsCommentMatch1+"\n");
			
			String markComment = "";
			
			if(htmlCommentMatch != "") {
				htmlCommentMatchString = "\n===\n*html-comment*:\n"+htmlCommentMatch+"\n===\n";
				//markInfoText.setText(htmlCommentMatchString.getBytes());
			}
			
			if(jsCommentMatch != "") {
				jsCommentMatchString = "\n===\n*js-comment*:\n"+jsCommentMatch+"\n===\n";
				//markInfoText.setText(jsCommentMatchString.getBytes());
			}
			
			if(jsCommentMatch1 != "") {
				jsCommentMatchString1 = "\n===\n*js-comment1*:\n"+jsCommentMatch1+"\n===\n";
				//markInfoText.setText(jsCommentMatchString1.getBytes());
			}
			
			markComment = htmlCommentMatchString + jsCommentMatchString + jsCommentMatchString1;
			markInfoText.setText(markComment.getBytes());
			
				
			
			currentMessage = content;
		}
	}

}
	

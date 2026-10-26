package com.yonlab.vision;
import java.util.*;import java.util.regex.*;

/** Deterministic fields retain their source lines; a missing amount is never guessed. */
final class DocumentFields {
 static final class Fields {String merchant="",date="",total="",currency="",totalSource="";int nonemptyLines;}
 static Fields parse(String text){Fields f=new Fields();String[] lines=(text==null?"":text).replace('\u00a0',' ').split("\\r?\\n");
  Pattern date=Pattern.compile("(20\\d{2})\\s*(?:[-/.]|년)\\s*(\\d{1,2})\\s*(?:[-/.]|월)\\s*(\\d{1,2})\\s*일?");
  Pattern amount=Pattern.compile("(?:[₩$€£]\\s*)?([0-9]{1,3}(?:[, ]?[0-9]{3})+(?:\\.[0-9]{2})?|[0-9]+(?:\\.[0-9]{2})?)\\s*(?:원|KRW|USD|EUR)?",Pattern.CASE_INSENSITIVE);
  Pattern totalLabel=Pattern.compile("(?:총액|총합계|결제금액|받을금액|합계|amount\\s*due|grand\\s*total|total)",Pattern.CASE_INSENSITIVE);
  for(int line=0;line<lines.length;line++){String s=lines[line].trim();if(s.isEmpty())continue;f.nonemptyLines++;Matcher dm=date.matcher(s);if(f.date.isEmpty()&&dm.find()){int y=Integer.parseInt(dm.group(1)),m=Integer.parseInt(dm.group(2)),d=Integer.parseInt(dm.group(3));GregorianCalendar cal=new GregorianCalendar();cal.setLenient(false);cal.clear();cal.set(y,m-1,d);try{cal.getTime();f.date=String.format(Locale.ROOT,"%04d-%02d-%02d",y,m,d);}catch(IllegalArgumentException ignored){}}
   String lower=s.toLowerCase(Locale.ROOT);Matcher label=totalLabel.matcher(s);boolean totalLine=label.find()&&!lower.matches(".*(?:subtotal|sub-total|소계|공급가액).*" );
   if(totalLine){String source=s;Matcher a=amount.matcher(source.substring(label.end()));String last=a.find()?a.group(1).replace(" ",""):"";if(last.isEmpty()&&line+1<lines.length){String next=lines[line+1].trim();if(next.matches("(?i)^[₩$€£]?\\s*[0-9][0-9,. ]*\\s*(?:원|KRW|USD|EUR|GBP)?$")){source=s+" "+next;a=amount.matcher(next);if(a.find())last=a.group(1).replace(" ","");}}if(!last.isEmpty()){f.total=last;f.totalSource=source;String textLower=source.toLowerCase(Locale.ROOT);f.currency=source.contains("$")||textLower.contains("usd")?"USD":source.contains("€")||textLower.contains("eur")?"EUR":source.contains("£")?"GBP":source.contains("원")||source.contains("₩")||textLower.contains("krw")?"KRW":"";}}
   if(f.merchant.isEmpty()&&s.length()>=3&&s.length()<=100&&!lower.matches(".*(?:20\\d\\d|영수증|receipt|합계|total|전화|tel|사업자|거래|승인).*" )&&s.matches(".*[A-Za-z가-힣].*"))f.merchant=s;
  }return f;
 }
}

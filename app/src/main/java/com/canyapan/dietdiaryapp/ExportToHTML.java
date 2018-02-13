package com.canyapan.dietdiaryapp;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.Xml;

import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.models.Event;

import org.joda.time.LocalDate;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

class ExportToHTML extends ExportAsyncTask {
    private static final String TAG = "ExportToHTML";

    private static final String NS_XHTML = "http://www.w3.org/1999/xhtml";

    private XmlSerializer serializer;
    private String[] types = null,
            foodTypes = null,
            drinkTypes = null;

    ExportToHTML(ExportActivity exportActivity, int destination, LocalDate fromDate, LocalDate toDate, OnExportListener listener) throws ExportException {
        super(exportActivity, destination, fromDate, toDate, listener);
    }

    @Override
    protected String getFileName(String prepend, LocalDate startDate, LocalDate endDate) {
        return MessageFormat.format("{0} {1} {2}.html", prepend,
                startDate.toString("yyyy-MM-dd"),
                endDate.toString("yyyy-MM-dd"));
    }

    @Override
    protected void start(final OutputStream outputStream, final LocalDate fromDate, final LocalDate toDate) throws IOException, ExportException {
        serializer = Xml.newSerializer();
        serializer.setOutput(outputStream, "UTF-8");
        serializer.docdecl(" html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\"");
        serializer.setPrefix("", NS_XHTML);

/*
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <meta name="viewport" content="width=device-width">
    <title>Diet Diary, 01/01/2017 – 31/12/2017</title>

    <style>
      ...
    </style>

    <style>
      ...
    </style>
  </head>
*/

        serializer.startTag(NS_XHTML, "html")
                .startTag("", "head")

                .startTag("", "meta")
                .attribute("", "http-equiv", "content-type")
                .attribute("", "content", "text/html; charset=utf-8")
                .endTag("", "meta")

                .startTag("", "meta")
                .attribute("", "name", "viewport")
                .attribute("", "content", "width=device-width")
                .endTag("", "meta")

                .startTag("", "title")
                .text(
                        MessageFormat.format("Diet Diary, {0} – {1}",
                                fromDate.toString("dd/MM/yyyy"),
                                toDate.toString("dd/MM/yyyy")))
                .endTag("", "title")

                //.startTag("", "link")
                //.attribute("", "rel", "stylesheet")
                //.attribute("", "href", "css/foundation-emails.css")
                //.endTag("", "link")
                .startTag("", "style").text(getFoundationEmailsCSS()).endTag("", "style")
                .startTag("", "style")
                .text("body,html,.body{background:#f3f3f3!important}\n" +
                        ".container.header{background:#f3f3f3}\n" +
                        ".body-border{border-top:8px solid #2a4964}\n" +
                        "hr{border:1px solid #2a4964}\n" +
                        "h1,h2,h3{color:#00223a}\n" +
                        "h4{text-decoration:underline}\n" +
                        "table th{font-weight:700}\n" +
                        "table .hour{width:10%}\n" +
                        "table .title{width:20%}\n" +
                        "table .desc{width:70%}\n")
                .endTag("", "style")
                .endTag("", "head")

/*<body>
    <table class="body" data-made-with-foundation="">
      <tr>
        <td class="float-center" align="center" valign="top">
          <center data-parsed="">
            <table class="spacer float-center">
              <tbody>
                <tr>
                  <td height="16px" style="font-size:16px;line-height:16px;">&#xA0;</td>
                </tr>
              </tbody>
            </table>
*/
                .startTag("", "body")
                .startTag("", "table")
                .attribute("", "class", "body")
                .attribute("", "data-made-with-foundation", "")

                .startTag("", "tr")
                .startTag("", "td")
                .attribute("", "class", "float-center")
                .attribute("", "align", "center")
                .attribute("", "valign", "top")
                .startTag("", "center").attribute("", "data-parsed", "")

                .startTag("", "table")
                .attribute("", "class", "spacer float-center")
                .startTag("", "tbody")
                .startTag("", "tr")
                .startTag("", "td")
                .attribute("", "height", "16px")
                .attribute("", "style", "font-size:16px;line-height:16px;")
                .text("\u00A0")
                .endTag("", "td")
                .endTag("", "tr")
                .endTag("", "tbody")
                .endTag("", "table")

/*          <table align="center" class="container header float-center">
              <tbody>
                <tr>
                  <td>

                    <table class="row">
                      <tbody>
                        <tr>
                          <th class="small-12 large-3 columns first">
                            <table>
                              <tr>
                                <th>
                                  <center data-parsed=""> <img src="icon.jpg" align="center" class="float-center"> </center>
                                </th>
                              </tr>
                            </table>
                          </th>
                          <th class="small-12 large-9 columns last">
                            <table>
                              <tr>
                                <th>
                                  <h1 class="text-center">Diet Diary</h1>
                                  <h3 class="text-center">01/01/2017 – 31/12/2017</h3>
                                </th>
                              </tr>
                            </table>
                          </th>
						  <th class="expander"></th>
                        </tr>
                      </tbody>
                    </table>

                  </td>
                </tr>
              </tbody>
            </table>
*/
                .startTag("", "table")
                .attribute("", "align", "center")
                .attribute("", "class", "container header float-center")
                .startTag("", "tbody")
                .startTag("", "tr")
                .startTag("", "td")

                .startTag("", "table").attribute("", "class", "row")
                .startTag("", "tbody")
                .startTag("", "tr")
                .startTag("", "th").attribute("", "class", "small-12 large-3 columns first")
                .startTag("", "table").startTag("", "tr").startTag("", "th").startTag("", "center")
                .startTag("", "img")
                .attribute("", "src", "icon.jpg")
                .attribute("", "align", "center")
                .attribute("", "class", "float-center")
                .endTag("", "img")
                .endTag("", "center").endTag("", "th").endTag("", "tr").endTag("", "table").endTag("", "th")
                .startTag("", "th").attribute("", "class", "small-12 large-9 columns last")
                .startTag("", "table").startTag("", "tr").startTag("", "th")
                .startTag("", "h1").attribute("", "class", "text-center").text("Diet Diary").endTag("", "h1")
                .startTag("", "h3").attribute("", "class", "text-center")
                .text(
                        MessageFormat.format("Diet Diary, {0} – {1}",
                                fromDate.toString("dd/MM/yyyy"),
                                toDate.toString("dd/MM/yyyy")))
                .endTag("", "h3")
                .endTag("", "th").endTag("", "tr").endTag("", "table").endTag("", "th")
                .startTag("", "th").attribute("", "class", "expander").endTag("", "th")
                .endTag("", "tr")
                .endTag("", "tbody")
                .endTag("", "table")

                .endTag("", "td")
                .endTag("", "tr")
                .endTag("", "tbody")
                .endTag("", "table")

/*          <table align="center" class="container body-border float-center">
              <tbody>
                <tr>
                  <td>
                    <table class="spacer">
                      <tbody>
                        <tr>
                          <td height="16px" style="font-size:16px;line-height:16px;">&#xA0;</td>
                        </tr>
                      </tbody>
                    </table>
*/
                .startTag("", "table")
                .attribute("", "align", "center")
                .attribute("", "class", "container body-border float-center")
                .startTag("", "tbody")
                .startTag("", "tr")
                .startTag("", "td")
                .startTag("", "table").attribute("", "class", "spacer")
                .startTag("", "tbody")
                .startTag("", "tr")
                .startTag("", "td")
                .attribute("", "height", "16px")
                .attribute("", "style", "font-size:16px;line-height:16px;")
                .text("\u00A0")
                .endTag("", "td")
                .endTag("", "tr")
                .endTag("", "tbody")
                .endTag("", "table")

/*                  <table class="row">
                      <tbody>
*/
                .startTag("", "table").attribute("", "class", "row")
                .startTag("", "tbody");
        // this tag will be closed in end() function.
        // inner tr tags will be filled in write() function.

        final Resources resources = getResources();
        types = resources.getStringArray(R.array.spinner_event_types);
        foodTypes = resources.getStringArray(R.array.spinner_event_food_types);
        drinkTypes = resources.getStringArray(R.array.spinner_event_drink_types);
    }

    @NonNull
    private String getFoundationEmailsCSS() {
        return ".wrapper{width:100%}\n" +
                "#outlook a{padding:0}\n" +
                "body{width:100%!important;min-width:100%;-webkit-text-size-adjust:100%;-ms-text-size-adjust:100%;margin:0;padding:0;-moz-box-sizing:border-box;-webkit-box-sizing:border-box;box-sizing:border-box}\n" +
                ".ExternalClass{width:100%}\n" +
                ".ExternalClass,.ExternalClass p,.ExternalClass span,.ExternalClass font,.ExternalClass td,.ExternalClass div{line-height:100%}\n" +
                "#backgroundTable{margin:0;padding:0;width:100%!important;line-height:100%!important}\n" +
                "img{outline:none;text-decoration:none;-ms-interpolation-mode:bicubic;width:auto;max-width:100%;clear:both;display:block}\n" +
                "center{width:100%;min-width:580px}\n" +
                "a img{border:none}\n" +
                "p{margin:0 0 0 10px}\n" +
                "table{border-spacing:0;border-collapse:collapse}\n" +
                "td{word-wrap:break-word;-webkit-hyphens:auto;-moz-hyphens:auto;hyphens:auto;border-collapse:collapse!important}\n" +
                "table,tr,td{padding:0;vertical-align:top;text-align:left}\n" +
                "@media only screen {\n" +
                "html{min-height:100%;background:#f3f3f3}\n" +
                "}\n" +
                "table.body{background:#f3f3f3;height:100%;width:100%}\n" +
                "table.container{background:#fefefe;width:580px;margin:0 auto;text-align:inherit}\n" +
                "table.row{padding:0;width:100%;position:relative}\n" +
                "table.spacer{width:100%}\n" +
                "table.spacer td{mso-line-height-rule:exactly}\n" +
                "table.container table.row{display:table}\n" +
                "td.columns,td.column,th.columns,th.column{margin:0 auto;padding-left:16px;padding-bottom:16px}\n" +
                "td.columns .column,td.columns .columns,td.column .column,td.column .columns,th.columns .column,th.columns .columns,th.column .column,th.column .columns{padding-left:0!important;padding-right:0!important}\n" +
                "td.columns .column center,td.columns .columns center,td.column .column center,td.column .columns center,th.columns .column center,th.columns .columns center,th.column .column center,th.column .columns center{min-width:none!important}\n" +
                "td.columns.last,td.column.last,th.columns.last,th.column.last{padding-right:16px}\n" +
                "td.columns table:not(.button),td.column table:not(.button),th.columns table:not(.button),th.column table:not(.button){width:100%}\n" +
                "td.large-1,th.large-1{width:32.33333px;padding-left:8px;padding-right:8px}\n" +
                "td.large-1.first,th.large-1.first{padding-left:16px}\n" +
                "td.large-1.last,th.large-1.last{padding-right:16px}\n" +
                ".collapse > tbody > tr > td.large-1,.collapse > tbody > tr > th.large-1{padding-right:0;padding-left:0;width:48.33333px}\n" +
                ".collapse td.large-1.first,.collapse th.large-1.first,.collapse td.large-1.last,.collapse th.large-1.last{width:56.33333px}\n" +
                "td.large-1 center,th.large-1 center{min-width:.33333px}\n" +
                ".body .columns td.large-1,.body .column td.large-1,.body .columns th.large-1,.body .column th.large-1{width:8.33333%}\n" +
                "td.large-2,th.large-2{width:80.66667px;padding-left:8px;padding-right:8px}\n" +
                "td.large-2.first,th.large-2.first{padding-left:16px}\n" +
                "td.large-2.last,th.large-2.last{padding-right:16px}\n" +
                ".collapse > tbody > tr > td.large-2,.collapse > tbody > tr > th.large-2{padding-right:0;padding-left:0;width:96.66667px}\n" +
                ".collapse td.large-2.first,.collapse th.large-2.first,.collapse td.large-2.last,.collapse th.large-2.last{width:104.66667px}\n" +
                "td.large-2 center,th.large-2 center{min-width:48.66667px}\n" +
                ".body .columns td.large-2,.body .column td.large-2,.body .columns th.large-2,.body .column th.large-2{width:16.66667%}\n" +
                "td.large-3,th.large-3{width:129px;padding-left:8px;padding-right:8px}\n" +
                "td.large-3.first,th.large-3.first{padding-left:16px}\n" +
                "td.large-3.last,th.large-3.last{padding-right:16px}\n" +
                ".collapse > tbody > tr > td.large-3,.collapse > tbody > tr > th.large-3{padding-right:0;padding-left:0;width:145px}\n" +
                ".collapse td.large-3.first,.collapse th.large-3.first,.collapse td.large-3.last,.collapse th.large-3.last{width:153px}\n" +
                "td.large-3 center,th.large-3 center{min-width:97px}\n" +
                ".body .columns td.large-3,.body .column td.large-3,.body .columns th.large-3,.body .column th.large-3{width:25%}\n" +
                "td.large-4,th.large-4{width:177.33333px;padding-left:8px;padding-right:8px}\n" +
                "td.large-4.first,th.large-4.first{padding-left:16px}\n" +
                "td.large-4.last,th.large-4.last{padding-right:16px}\n" +
                ".collapse > tbody > tr > td.large-4,.collapse > tbody > tr > th.large-4{padding-right:0;padding-left:0;width:193.33333px}\n" +
                ".collapse td.large-4.first,.collapse th.large-4.first,.collapse td.large-4.last,.collapse th.large-4.last{width:201.33333px}\n" +
                "td.large-4 center,th.large-4 center{min-width:145.33333px}\n" +
                ".body .columns td.large-4,.body .column td.large-4,.body .columns th.large-4,.body .column th.large-4{width:33.33333%}\n" +
                "td.large-5,th.large-5{width:225.66667px;padding-left:8px;padding-right:8px}\n" +
                "td.large-5.first,th.large-5.first{padding-left:16px}\n" +
                "td.large-5.last,th.large-5.last{padding-right:16px}\n" +
                ".collapse > tbody > tr > td.large-5,.collapse > tbody > tr > th.large-5{padding-right:0;padding-left:0;width:241.66667px}\n" +
                ".collapse td.large-5.first,.collapse th.large-5.first,.collapse td.large-5.last,.collapse th.large-5.last{width:249.66667px}\n" +
                "td.large-5 center,th.large-5 center{min-width:193.66667px}\n" +
                ".body .columns td.large-5,.body .column td.large-5,.body .columns th.large-5,.body .column th.large-5{width:41.66667%}\n" +
                "td.large-6,th.large-6{width:274px;padding-left:8px;padding-right:8px}\n" +
                "td.large-6.first,th.large-6.first{padding-left:16px}\n" +
                "td.large-6.last,th.large-6.last{padding-right:16px}\n" +
                ".collapse > tbody > tr > td.large-6,.collapse > tbody > tr > th.large-6{padding-right:0;padding-left:0;width:290px}\n" +
                ".collapse td.large-6.first,.collapse th.large-6.first,.collapse td.large-6.last,.collapse th.large-6.last{width:298px}\n" +
                "td.large-6 center,th.large-6 center{min-width:242px}\n" +
                ".body .columns td.large-6,.body .column td.large-6,.body .columns th.large-6,.body .column th.large-6{width:50%}\n" +
                "td.large-7,th.large-7{width:322.33333px;padding-left:8px;padding-right:8px}\n" +
                "td.large-7.first,th.large-7.first{padding-left:16px}\n" +
                "td.large-7.last,th.large-7.last{padding-right:16px}\n" +
                ".collapse > tbody > tr > td.large-7,.collapse > tbody > tr > th.large-7{padding-right:0;padding-left:0;width:338.33333px}\n" +
                ".collapse td.large-7.first,.collapse th.large-7.first,.collapse td.large-7.last,.collapse th.large-7.last{width:346.33333px}\n" +
                "td.large-7 center,th.large-7 center{min-width:290.33333px}\n" +
                ".body .columns td.large-7,.body .column td.large-7,.body .columns th.large-7,.body .column th.large-7{width:58.33333%}\n" +
                "td.large-8,th.large-8{width:370.66667px;padding-left:8px;padding-right:8px}\n" +
                "td.large-8.first,th.large-8.first{padding-left:16px}\n" +
                "td.large-8.last,th.large-8.last{padding-right:16px}\n" +
                ".collapse > tbody > tr > td.large-8,.collapse > tbody > tr > th.large-8{padding-right:0;padding-left:0;width:386.66667px}\n" +
                ".collapse td.large-8.first,.collapse th.large-8.first,.collapse td.large-8.last,.collapse th.large-8.last{width:394.66667px}\n" +
                "td.large-8 center,th.large-8 center{min-width:338.66667px}\n" +
                ".body .columns td.large-8,.body .column td.large-8,.body .columns th.large-8,.body .column th.large-8{width:66.66667%}\n" +
                "td.large-9,th.large-9{width:419px;padding-left:8px;padding-right:8px}\n" +
                "td.large-9.first,th.large-9.first{padding-left:16px}\n" +
                "td.large-9.last,th.large-9.last{padding-right:16px}\n" +
                ".collapse > tbody > tr > td.large-9,.collapse > tbody > tr > th.large-9{padding-right:0;padding-left:0;width:435px}\n" +
                ".collapse td.large-9.first,.collapse th.large-9.first,.collapse td.large-9.last,.collapse th.large-9.last{width:443px}\n" +
                "td.large-9 center,th.large-9 center{min-width:387px}\n" +
                ".body .columns td.large-9,.body .column td.large-9,.body .columns th.large-9,.body .column th.large-9{width:75%}\n" +
                "td.large-10,th.large-10{width:467.33333px;padding-left:8px;padding-right:8px}\n" +
                "td.large-10.first,th.large-10.first{padding-left:16px}\n" +
                "td.large-10.last,th.large-10.last{padding-right:16px}\n" +
                ".collapse > tbody > tr > td.large-10,.collapse > tbody > tr > th.large-10{padding-right:0;padding-left:0;width:483.33333px}\n" +
                ".collapse td.large-10.first,.collapse th.large-10.first,.collapse td.large-10.last,.collapse th.large-10.last{width:491.33333px}\n" +
                "td.large-10 center,th.large-10 center{min-width:435.33333px}\n" +
                ".body .columns td.large-10,.body .column td.large-10,.body .columns th.large-10,.body .column th.large-10{width:83.33333%}\n" +
                "td.large-11,th.large-11{width:515.66667px;padding-left:8px;padding-right:8px}\n" +
                "td.large-11.first,th.large-11.first{padding-left:16px}\n" +
                "td.large-11.last,th.large-11.last{padding-right:16px}\n" +
                ".collapse > tbody > tr > td.large-11,.collapse > tbody > tr > th.large-11{padding-right:0;padding-left:0;width:531.66667px}\n" +
                ".collapse td.large-11.first,.collapse th.large-11.first,.collapse td.large-11.last,.collapse th.large-11.last{width:539.66667px}\n" +
                "td.large-11 center,th.large-11 center{min-width:483.66667px}\n" +
                ".body .columns td.large-11,.body .column td.large-11,.body .columns th.large-11,.body .column th.large-11{width:91.66667%}\n" +
                "td.large-12,th.large-12{width:564px;padding-left:8px;padding-right:8px}\n" +
                "td.large-12.first,th.large-12.first{padding-left:16px}\n" +
                "td.large-12.last,th.large-12.last{padding-right:16px}\n" +
                ".collapse > tbody > tr > td.large-12,.collapse > tbody > tr > th.large-12{padding-right:0;padding-left:0;width:580px}\n" +
                ".collapse td.large-12.first,.collapse th.large-12.first,.collapse td.large-12.last,.collapse th.large-12.last{width:588px}\n" +
                "td.large-12 center,th.large-12 center{min-width:532px}\n" +
                ".body .columns td.large-12,.body .column td.large-12,.body .columns th.large-12,.body .column th.large-12{width:100%}\n" +
                "td.large-offset-1,td.large-offset-1.first,td.large-offset-1.last,th.large-offset-1,th.large-offset-1.first,th.large-offset-1.last{padding-left:64.33333px}\n" +
                "td.large-offset-2,td.large-offset-2.first,td.large-offset-2.last,th.large-offset-2,th.large-offset-2.first,th.large-offset-2.last{padding-left:112.66667px}\n" +
                "td.large-offset-3,td.large-offset-3.first,td.large-offset-3.last,th.large-offset-3,th.large-offset-3.first,th.large-offset-3.last{padding-left:161px}\n" +
                "td.large-offset-4,td.large-offset-4.first,td.large-offset-4.last,th.large-offset-4,th.large-offset-4.first,th.large-offset-4.last{padding-left:209.33333px}\n" +
                "td.large-offset-5,td.large-offset-5.first,td.large-offset-5.last,th.large-offset-5,th.large-offset-5.first,th.large-offset-5.last{padding-left:257.66667px}\n" +
                "td.large-offset-6,td.large-offset-6.first,td.large-offset-6.last,th.large-offset-6,th.large-offset-6.first,th.large-offset-6.last{padding-left:306px}\n" +
                "td.large-offset-7,td.large-offset-7.first,td.large-offset-7.last,th.large-offset-7,th.large-offset-7.first,th.large-offset-7.last{padding-left:354.33333px}\n" +
                "td.large-offset-8,td.large-offset-8.first,td.large-offset-8.last,th.large-offset-8,th.large-offset-8.first,th.large-offset-8.last{padding-left:402.66667px}\n" +
                "td.large-offset-9,td.large-offset-9.first,td.large-offset-9.last,th.large-offset-9,th.large-offset-9.first,th.large-offset-9.last{padding-left:451px}\n" +
                "td.large-offset-10,td.large-offset-10.first,td.large-offset-10.last,th.large-offset-10,th.large-offset-10.first,th.large-offset-10.last{padding-left:499.33333px}\n" +
                "td.large-offset-11,td.large-offset-11.first,td.large-offset-11.last,th.large-offset-11,th.large-offset-11.first,th.large-offset-11.last{padding-left:547.66667px}\n" +
                "td.expander,th.expander{visibility:hidden;width:0;padding:0!important}\n" +
                "table.container.radius{border-radius:0;border-collapse:separate}\n" +
                ".block-grid{width:100%;max-width:580px}\n" +
                ".block-grid td{display:inline-block;padding:8px}\n" +
                ".up-2 td{width:274px!important}\n" +
                ".up-3 td{width:177px!important}\n" +
                ".up-4 td{width:129px!important}\n" +
                ".up-5 td{width:100px!important}\n" +
                ".up-6 td{width:80px!important}\n" +
                ".up-7 td{width:66px!important}\n" +
                ".up-8 td{width:56px!important}\n" +
                "table.text-center,th.text-center,td.text-center,h1.text-center,h2.text-center,h3.text-center,h4.text-center,h5.text-center,h6.text-center,p.text-center,span.text-center{text-align:center}\n" +
                "table.text-left,th.text-left,td.text-left,h1.text-left,h2.text-left,h3.text-left,h4.text-left,h5.text-left,h6.text-left,p.text-left,span.text-left{text-align:left}\n" +
                "table.text-right,th.text-right,td.text-right,h1.text-right,h2.text-right,h3.text-right,h4.text-right,h5.text-right,h6.text-right,p.text-right,span.text-right{text-align:right}\n" +
                "span.text-center{display:block;width:100%;text-align:center}\n" +
                "@media only screen and (max-width: 596px) {\n" +
                ".small-float-center{margin:0 auto!important;float:none!important;text-align:center!important}\n" +
                ".small-text-center{text-align:center!important}\n" +
                ".small-text-left{text-align:left!important}\n" +
                ".small-text-right{text-align:right!important}\n" +
                "}\n" +
                "img.float-left{float:left;text-align:left}\n" +
                "img.float-right{float:right;text-align:right}\n" +
                "img.float-center,img.text-center{margin:0 auto;float:none;text-align:center}\n" +
                "table.float-center,td.float-center,th.float-center{margin:0 auto;float:none;text-align:center}\n" +
                ".hide-for-large{display:none!important;mso-hide:all;overflow:hidden;max-height:0;font-size:0;width:0;line-height:0}\n" +
                "@media only screen and (max-width: 596px) {\n" +
                ".hide-for-large{display:block!important;width:auto!important;overflow:visible!important;max-height:none!important;font-size:inherit!important;line-height:inherit!important}\n" +
                "}\n" +
                "table.body table.container .hide-for-large *{mso-hide:all}\n" +
                "@media only screen and (max-width: 596px) {\n" +
                "table.body table.container .hide-for-large,table.body table.container .row.hide-for-large{display:table!important;width:100%!important}\n" +
                "}\n" +
                "@media only screen and (max-width: 596px) {\n" +
                "table.body table.container .callout-inner.hide-for-large{display:table-cell!important;width:100%!important}\n" +
                "}\n" +
                "@media only screen and (max-width: 596px) {\n" +
                "table.body table.container .show-for-large{display:none!important;width:0;mso-hide:all;overflow:hidden}\n" +
                "}\n" +
                "body,table.body,h1,h2,h3,h4,h5,h6,p,td,th,a{color:#0a0a0a;font-family:Helvetica,Arial,sans-serif;font-weight:400;padding:0;margin:0;text-align:left;line-height:1.3}\n" +
                "h1,h2,h3,h4,h5,h6{color:inherit;word-wrap:normal;font-family:Helvetica,Arial,sans-serif;font-weight:400;margin-bottom:10px}\n" +
                "h1{font-size:34px}\n" +
                "h2{font-size:30px}\n" +
                "h3{font-size:28px}\n" +
                "h4{font-size:24px}\n" +
                "h5{font-size:20px}\n" +
                "h6{font-size:18px}\n" +
                "body,table.body,p,td,th{font-size:16px;line-height:1.3}\n" +
                "p{margin-bottom:10px}\n" +
                "p.lead{font-size:20px;line-height:1.6}\n" +
                "p.subheader{margin-top:4px;margin-bottom:8px;font-weight:400;line-height:1.4;color:#8a8a8a}\n" +
                "small{font-size:80%;color:#cacaca}\n" +
                "a{color:#2199e8;text-decoration:none}\n" +
                "a:hover{color:#147dc2}\n" +
                "a:active{color:#147dc2}\n" +
                "a:visited{color:#2199e8}\n" +
                "h1 a,h1 a:visited,h2 a,h2 a:visited,h3 a,h3 a:visited,h4 a,h4 a:visited,h5 a,h5 a:visited,h6 a,h6 a:visited{color:#2199e8}\n" +
                "pre{background:#f3f3f3;margin:30px 0}\n" +
                "pre code{color:#cacaca}\n" +
                "pre code span.callout{color:#8a8a8a;font-weight:700}\n" +
                "pre code span.callout-strong{color:#ff6908;font-weight:700}\n" +
                "table.hr{width:100%}\n" +
                "table.hr th{height:0;max-width:580px;border-top:0;border-right:0;border-bottom:1px solid #0a0a0a;border-left:0;margin:20px auto;clear:both}\n" +
                ".stat{font-size:40px;line-height:1}\n" +
                "p + .stat{margin-top:-16px}\n" +
                "span.preheader{display:none!important;visibility:hidden;mso-hide:all!important;font-size:1px;color:#f3f3f3;line-height:1px;max-height:0;max-width:0;opacity:0;overflow:hidden}\n" +
                "table.button{width:auto;margin:0 0 16px}\n" +
                "table.button table td{text-align:left;color:#fefefe;background:#2199e8;border:2px solid #2199e8}\n" +
                "table.button table td a{font-family:Helvetica,Arial,sans-serif;font-size:16px;font-weight:700;color:#fefefe;text-decoration:none;display:inline-block;padding:8px 16px;border:0 solid #2199e8;border-radius:3px}\n" +
                "table.button.radius table td{border-radius:3px;border:none}\n" +
                "table.button.rounded table td{border-radius:500px;border:none}\n" +
                "table.button:hover table tr td a,table.button:active table tr td a,table.button table tr td a:visited,table.button.tiny:hover table tr td a,table.button.tiny:active table tr td a,table.button.tiny table tr td a:visited,table.button.small:hover table tr td a,table.button.small:active table tr td a,table.button.small table tr td a:visited,table.button.large:hover table tr td a,table.button.large:active table tr td a,table.button.large table tr td a:visited{color:#fefefe}\n" +
                "table.button.tiny table td,table.button.tiny table a{padding:4px 8px}\n" +
                "table.button.tiny table a{font-size:10px;font-weight:400}\n" +
                "table.button.small table td,table.button.small table a{padding:5px 10px;font-size:12px}\n" +
                "table.button.large table a{padding:10px 20px;font-size:20px}\n" +
                "table.button.expand,table.button.expanded{width:100%!important}\n" +
                "table.button.expand table,table.button.expanded table{width:100%}\n" +
                "table.button.expand table a,table.button.expanded table a{text-align:center;width:100%;padding-left:0;padding-right:0}\n" +
                "table.button.expand center,table.button.expanded center{min-width:0}\n" +
                "table.button:hover table td,table.button:visited table td,table.button:active table td{background:#147dc2;color:#fefefe}\n" +
                "table.button:hover table a,table.button:visited table a,table.button:active table a{border:0 solid #147dc2}\n" +
                "table.button.secondary table td{background:#777;color:#fefefe;border:0 solid #777}\n" +
                "table.button.secondary table a{color:#fefefe;border:0 solid #777}\n" +
                "table.button.secondary:hover table td{background:#919191;color:#fefefe}\n" +
                "table.button.secondary:hover table a{border:0 solid #919191}\n" +
                "table.button.secondary:hover table td a{color:#fefefe}\n" +
                "table.button.secondary:active table td a{color:#fefefe}\n" +
                "table.button.secondary table td a:visited{color:#fefefe}\n" +
                "table.button.success table td{background:#3adb76;border:0 solid #3adb76}\n" +
                "table.button.success table a{border:0 solid #3adb76}\n" +
                "table.button.success:hover table td{background:#23bf5d}\n" +
                "table.button.success:hover table a{border:0 solid #23bf5d}\n" +
                "table.button.alert table td{background:#ec5840;border:0 solid #ec5840}\n" +
                "table.button.alert table a{border:0 solid #ec5840}\n" +
                "table.button.alert:hover table td{background:#e23317}\n" +
                "table.button.alert:hover table a{border:0 solid #e23317}\n" +
                "table.button.warning table td{background:#ffae00;border:0 solid #ffae00}\n" +
                "table.button.warning table a{border:0 solid #ffae00}\n" +
                "table.button.warning:hover table td{background:#cc8b00}\n" +
                "table.button.warning:hover table a{border:0 solid #cc8b00}\n" +
                "table.callout{margin-bottom:16px}\n" +
                "th.callout-inner{width:100%;border:1px solid #cbcbcb;padding:10px;background:#fefefe}\n" +
                "th.callout-inner.primary{background:#def0fc;border:1px solid #444;color:#0a0a0a}\n" +
                "th.callout-inner.secondary{background:#ebebeb;border:1px solid #444;color:#0a0a0a}\n" +
                "th.callout-inner.success{background:#e1faea;border:1px solid #1b9448;color:#fefefe}\n" +
                "th.callout-inner.warning{background:#fff3d9;border:1px solid #996800;color:#fefefe}\n" +
                "th.callout-inner.alert{background:#fce6e2;border:1px solid #b42912;color:#fefefe}\n" +
                ".thumbnail{border:solid 4px #fefefe;box-shadow:0 0 0 1px rgba(10,10,10,0.2);display:inline-block;line-height:0;max-width:100%;transition:box-shadow 200ms ease-out;border-radius:3px;margin-bottom:16px}\n" +
                ".thumbnail:hover,.thumbnail:focus{box-shadow:0 0 6px 1px rgba(33,153,232,0.5)}\n" +
                "table.menu{width:580px}\n" +
                "table.menu td.menu-item,table.menu th.menu-item{padding:10px;padding-right:10px}\n" +
                "table.menu td.menu-item a,table.menu th.menu-item a{color:#2199e8}\n" +
                "table.menu.vertical td.menu-item,table.menu.vertical th.menu-item{padding:10px;padding-right:0;display:block}\n" +
                "table.menu.vertical td.menu-item a,table.menu.vertical th.menu-item a{width:100%}\n" +
                "table.menu.vertical td.menu-item table.menu.vertical td.menu-item,table.menu.vertical td.menu-item table.menu.vertical th.menu-item,table.menu.vertical th.menu-item table.menu.vertical td.menu-item,table.menu.vertical th.menu-item table.menu.vertical th.menu-item{padding-left:10px}\n" +
                "table.menu.text-center a{text-align:center}\n" +
                ".menu[align=\"center\"]{width:auto!important}\n" +
                "body.outlook p{display:inline!important}\n" +
                "@media only screen and (max-width: 596px) {\n" +
                "table.body img{width:auto;height:auto}\n" +
                "table.body center{min-width:0!important}\n" +
                "table.body .container{width:95%!important}\n" +
                "table.body .columns,table.body .column{height:auto!important;-moz-box-sizing:border-box;-webkit-box-sizing:border-box;box-sizing:border-box;padding-left:16px!important;padding-right:16px!important}\n" +
                "table.body .columns .column,table.body .columns .columns,table.body .column .column,table.body .column .columns{padding-left:0!important;padding-right:0!important}\n" +
                "table.body .collapse .columns,table.body .collapse .column{padding-left:0!important;padding-right:0!important}\n" +
                "td.small-1,th.small-1{display:inline-block!important;width:8.33333%!important}\n" +
                "td.small-2,th.small-2{display:inline-block!important;width:16.66667%!important}\n" +
                "td.small-3,th.small-3{display:inline-block!important;width:25%!important}\n" +
                "td.small-4,th.small-4{display:inline-block!important;width:33.33333%!important}\n" +
                "td.small-5,th.small-5{display:inline-block!important;width:41.66667%!important}\n" +
                "td.small-6,th.small-6{display:inline-block!important;width:50%!important}\n" +
                "td.small-7,th.small-7{display:inline-block!important;width:58.33333%!important}\n" +
                "td.small-8,th.small-8{display:inline-block!important;width:66.66667%!important}\n" +
                "td.small-9,th.small-9{display:inline-block!important;width:75%!important}\n" +
                "td.small-10,th.small-10{display:inline-block!important;width:83.33333%!important}\n" +
                "td.small-11,th.small-11{display:inline-block!important;width:91.66667%!important}\n" +
                "td.small-12,th.small-12{display:inline-block!important;width:100%!important}\n" +
                ".columns td.small-12,.column td.small-12,.columns th.small-12,.column th.small-12{display:block!important;width:100%!important}\n" +
                "table.body td.small-offset-1,table.body th.small-offset-1{margin-left:8.33333%!important}\n" +
                "table.body td.small-offset-2,table.body th.small-offset-2{margin-left:16.66667%!important}\n" +
                "table.body td.small-offset-3,table.body th.small-offset-3{margin-left:25%!important}\n" +
                "table.body td.small-offset-4,table.body th.small-offset-4{margin-left:33.33333%!important}\n" +
                "table.body td.small-offset-5,table.body th.small-offset-5{margin-left:41.66667%!important}\n" +
                "table.body td.small-offset-6,table.body th.small-offset-6{margin-left:50%!important}\n" +
                "table.body td.small-offset-7,table.body th.small-offset-7{margin-left:58.33333%!important}\n" +
                "table.body td.small-offset-8,table.body th.small-offset-8{margin-left:66.66667%!important}\n" +
                "table.body td.small-offset-9,table.body th.small-offset-9{margin-left:75%!important}\n" +
                "table.body td.small-offset-10,table.body th.small-offset-10{margin-left:83.33333%!important}\n" +
                "table.body td.small-offset-11,table.body th.small-offset-11{margin-left:91.66667%!important}\n" +
                "table.body table.columns td.expander,table.body table.columns th.expander{display:none!important}\n" +
                "table.body .right-text-pad,table.body .text-pad-right{padding-left:10px!important}\n" +
                "table.body .left-text-pad,table.body .text-pad-left{padding-right:10px!important}\n" +
                "table.menu{width:100%!important}\n" +
                "table.menu td,table.menu th{width:auto!important;display:inline-block!important}\n" +
                "table.menu.vertical td,table.menu.vertical th,table.menu.small-vertical td,table.menu.small-vertical th{display:block!important}\n" +
                "table.menu[align=\"center\"]{width:auto!important}\n" +
                "table.button.small-expand,table.button.small-expanded{width:100%!important}\n" +
                "table.button.small-expand table,table.button.small-expanded table{width:100%}\n" +
                "table.button.small-expand table a,table.button.small-expanded table a{text-align:center!important;width:100%!important;padding-left:0!important;padding-right:0!important}\n" +
                "table.button.small-expand center,table.button.small-expanded center{min-width:0}\n" +
                "}\n";
    }

    @Override
    protected void write(final Event event, boolean newDay, final long index, final long count) throws IOException {
        String subType;
        switch (event.getType()) {
            case Event.TYPE_FOOD:
                subType = foodTypes[event.getSubType()];
                break;
            case Event.TYPE_DRINK:
                subType = drinkTypes[event.getSubType()];
                break;
            default:
                subType = "";
        }

        if (newDay) {
            if (index != 0) {
                closeLastDay();
            }

            beginDay(event.getDate());
        }

        /*                          <tr>
                                      <td>13:00</td>
                                      <td>Drink</td>
                                      <td>A lot of text</td>
                                    </tr>
         */

        serializer.startTag("", "tr")
                .startTag("", "td").text(event.getTime().toString(DatabaseHelper.DB_TIME_FORMATTER)).endTag("", "td")
                .startTag("", "td").text(subType).endTag("", "td")
                .startTag("", "td").text(event.getDescription()).endTag("", "td")
                .endTag("", "tr");

        if (index + 1 == count) {
/*                                </table>
                                </th>
                              </tr>
                            </table>
                          </th>
                        </tr>
*/
            serializer.endTag("", "table")
                    .endTag("", "th")
                    .endTag("", "tr")
                    .endTag("", "table")
                    .endTag("", "th")
                    .endTag("", "tr");
        }
    }

    private void beginDay(final LocalDate date) throws IOException {
        /*              <tr>
                          <th class="small-12 large-12 columns first last">
                            <table>
                              <tr>
                                <th>
                                  <h4>Mon, 01 Jan, 2017</h4>
                                  <table>
                                    <tr>
                                      <th class="hour">Hour</th>
									  <th class="title">Title</th>
                                      <th class="desc">Description</th>
                                    </tr>
         */

        serializer.startTag("", "tr")
                .startTag("", "th").attribute("", "class", "small-12 large-12 columns first last")
                .startTag("", "table")
                .startTag("", "tr")
                .startTag("", "th")
                .startTag("", "h4").text(date.toString("EEE, d MMM, yyyy")).endTag("", "h4")
                .startTag("", "table")
                .startTag("", "tr")
                .startTag("", "th").attribute("", "class", "hour").text("Hour").endTag("", "th")
                .startTag("", "th").attribute("", "class", "title").text("Title").endTag("", "th")
                .startTag("", "th").attribute("", "class", "desc").text("Description").endTag("", "th")
                .endTag("", "tr");
    }

    private void closeLastDay() throws IOException {
        /*                        </table>
                                </th>
                              </tr>
                            </table>
                          </th>
                        </tr>
         */

        serializer.endTag("", "table")
                .endTag("", "th")
                .endTag("", "tr")
                .endTag("", "table")
                .endTag("", "th")
                .endTag("", "tr");
    }

    @Override
    protected void end() throws IOException, ExportException {
/*                    </tbody>
                    </table>

                    <hr>
                    <table class="row">
                      <tbody>
                        <tr>
                          <th class="small-12 large-8 columns first">
                            <table>
                              <tr>
                                <th>
                                  <p><small>This data was exported from Diet Diary app for android.</small></p>
                                </th>
                              </tr>
                            </table>
                          </th>
                          <th class="small-12 large-4 columns last">
                            <table>
                              <tr>
                                <th>
                                  <img src="qrcode.jpg" class="float-right">
                                </th>
                              </tr>
                            </table>
                          </th>
                        </tr>
                      </tbody>
                    </table>
*/
        serializer.endTag("", "tbody")
                .endTag("", "table")

                .startTag("", "hr").endTag("", "hr")
                .startTag("", "table").attribute("", "class", "row")
                .startTag("", "tbody")
                .startTag("", "tr")

                .startTag("", "th").attribute("", "classs", "small-12 large-8 columns first")
                .startTag("", "table")
                .startTag("", "tr")
                .startTag("", "th")
                .startTag("", "p")
                .startTag("", "small")
                .text("This data was exported from Diet Diary app for android.")
                .endTag("", "small")
                .endTag("", "p")
                .endTag("", "th")
                .endTag("", "tr")
                .endTag("", "table")
                .endTag("", "th")

                .startTag("", "th").attribute("", "classs", "small-12 large-4 columns last")
                .startTag("", "table")
                .startTag("", "tr")
                .startTag("", "th")
                .startTag("", "img").attribute("", "src", "qrcode.jpg").attribute("", "class", "float-right").endTag("", "img")
                .endTag("", "th")
                .endTag("", "tr")
                .endTag("", "table")
                .endTag("", "th")

                .endTag("", "tr")
                .endTag("", "tbody")
                .endTag("", "table")

/*                  <table class="spacer">
                      <tbody>
                        <tr>
                          <td height="16px" style="font-size:16px;line-height:16px;">&#xA0;</td>
                        </tr>
                      </tbody>
                    </table>

                  </td>
                </tr>
              </tbody>
            </table>

          </center>
        </td>
      </tr>
    </table>

  </body>
</html>
*/

                .startTag("", "table").attribute("", "class", "spacer")
                .startTag("", "tbody")
                .startTag("", "tr")
                .startTag("", "td").attribute("", "height", "16px")
                .attribute("", "style", "font-size:16px;line-height:16px;")
                .text("\u00A0")
                .endTag("", "td")
                .endTag("", "tr")
                .endTag("", "tbody")
                .endTag("", "table")

                .endTag("", "td")
                .endTag("", "tr")
                .endTag("", "tbody")
                .endTag("", "table")

                .endTag("", "center")
                .endTag("", "td")
                .endTag("", "tr")
                .endTag("", "table")

                .endTag("", "body")
                .endTag(NS_XHTML, "html")
                .endDocument();

        serializer.flush();
    }

}

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
    protected void start(final OutputStream outputStream, final LocalDate fromDate, final LocalDate toDate) throws IOException {
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
                .startTag("", "style").attribute("", "type", "text/css")
                .text(getFoundationEmailsCSS()).endTag("", "style")
                .startTag("", "style").attribute("", "type", "text/css")
                .text("body, html, .body { background: #f3f3f3 !important; }\n" +
                        ".container.header { background: #f3f3f3; }\n" +
                        ".body-border { border-top: 8px solid #2a4964; }\n" +
                        "hr { border: 1px solid #2a4964; }\n" +
                        "h1, h2, h3 { color: #00223a; }\n" +
                        "h4 { text-decoration: underline; }\n" +
                        "table th { font-weight: bold; }\n" +
                        "table .hour { width: 10%; }\n" +
                        "table .title { width: 20%; }\n" +
                        "table .desc { width: 70%; }")
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
                .attribute("", "style", "font-size : 16px; line-height : 16px;")
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
                .attribute("", "src", "http://dietdiaryapp.canyapan.com/email/logo.jpg")
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
                .attribute("", "style", "font-size : 16px; line-height : 16px;")
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
        return ".wrapper {\n" +
                "  width: 100%; }\n" +
                "\n" +
                "#outlook a {\n" +
                "  padding: 0; }\n" +
                "\n" +
                "body {\n" +
                "  width: 100% !important;\n" +
                "  min-width: 100%;\n" +
                "  -webkit-text-size-adjust: 100%;\n" +
                "  -ms-text-size-adjust: 100%;\n" +
                "  margin: 0;\n" +
                "  Margin: 0;\n" +
                "  padding: 0;\n" +
                "  -moz-box-sizing: border-box;\n" +
                "  -webkit-box-sizing: border-box;\n" +
                "  box-sizing: border-box; }\n" +
                "\n" +
                ".ExternalClass {\n" +
                "  width: 100%; }\n" +
                "  .ExternalClass,\n" +
                "  .ExternalClass p,\n" +
                "  .ExternalClass span,\n" +
                "  .ExternalClass font,\n" +
                "  .ExternalClass td,\n" +
                "  .ExternalClass div {\n" +
                "    line-height: 100%; }\n" +
                "\n" +
                "#backgroundTable {\n" +
                "  margin: 0;\n" +
                "  Margin: 0;\n" +
                "  padding: 0;\n" +
                "  width: 100% !important;\n" +
                "  line-height: 100% !important; }\n" +
                "\n" +
                "img {\n" +
                "  outline: none;\n" +
                "  text-decoration: none;\n" +
                "  -ms-interpolation-mode: bicubic;\n" +
                "  width: auto;\n" +
                "  max-width: 100%;\n" +
                "  clear: both;\n" +
                "  display: block; }\n" +
                "\n" +
                "center {\n" +
                "  width: 100%;\n" +
                "  min-width: 580px; }\n" +
                "\n" +
                "a img {\n" +
                "  border: none; }\n" +
                "\n" +
                "p {\n" +
                "  margin: 0 0 0 10px;\n" +
                "  Margin: 0 0 0 10px; }\n" +
                "\n" +
                "table {\n" +
                "  border-spacing: 0;\n" +
                "  border-collapse: collapse; }\n" +
                "\n" +
                "td {\n" +
                "  word-wrap: break-word;\n" +
                "  -webkit-hyphens: auto;\n" +
                "  -moz-hyphens: auto;\n" +
                "  hyphens: auto;\n" +
                "  border-collapse: collapse !important; }\n" +
                "\n" +
                "table, tr, td {\n" +
                "  padding: 0;\n" +
                "  vertical-align: top;\n" +
                "  text-align: left; }\n" +
                "\n" +
                "@media only screen {\n" +
                "  html {\n" +
                "    min-height: 100%;\n" +
                "    background: #f3f3f3; } }\n" +
                "\n" +
                "table.body {\n" +
                "  background: #f3f3f3;\n" +
                "  height: 100%;\n" +
                "  width: 100%; }\n" +
                "\n" +
                "table.container {\n" +
                "  background: #fefefe;\n" +
                "  width: 580px;\n" +
                "  margin: 0 auto;\n" +
                "  Margin: 0 auto;\n" +
                "  text-align: inherit; }\n" +
                "\n" +
                "table.row {\n" +
                "  padding: 0;\n" +
                "  width: 100%;\n" +
                "  position: relative; }\n" +
                "\n" +
                "table.spacer {\n" +
                "  width: 100%; }\n" +
                "  table.spacer td {\n" +
                "    mso-line-height-rule: exactly; }\n" +
                "\n" +
                "table.container table.row {\n" +
                "  display: table; }\n" +
                "\n" +
                "td.columns,\n" +
                "td.column,\n" +
                "th.columns,\n" +
                "th.column {\n" +
                "  margin: 0 auto;\n" +
                "  Margin: 0 auto;\n" +
                "  padding-left: 16px;\n" +
                "  padding-bottom: 16px; }\n" +
                "  td.columns .column,\n" +
                "  td.columns .columns,\n" +
                "  td.column .column,\n" +
                "  td.column .columns,\n" +
                "  th.columns .column,\n" +
                "  th.columns .columns,\n" +
                "  th.column .column,\n" +
                "  th.column .columns {\n" +
                "    padding-left: 0 !important;\n" +
                "    padding-right: 0 !important; }\n" +
                "    td.columns .column center,\n" +
                "    td.columns .columns center,\n" +
                "    td.column .column center,\n" +
                "    td.column .columns center,\n" +
                "    th.columns .column center,\n" +
                "    th.columns .columns center,\n" +
                "    th.column .column center,\n" +
                "    th.column .columns center {\n" +
                "      min-width: none !important; }\n" +
                "\n" +
                "td.columns.last,\n" +
                "td.column.last,\n" +
                "th.columns.last,\n" +
                "th.column.last {\n" +
                "  padding-right: 16px; }\n" +
                "\n" +
                "td.columns table:not(.button),\n" +
                "td.column table:not(.button),\n" +
                "th.columns table:not(.button),\n" +
                "th.column table:not(.button) {\n" +
                "  width: 100%; }\n" +
                "\n" +
                "td.large-1,\n" +
                "th.large-1 {\n" +
                "  width: 32.33333px;\n" +
                "  padding-left: 8px;\n" +
                "  padding-right: 8px; }\n" +
                "\n" +
                "td.large-1.first,\n" +
                "th.large-1.first {\n" +
                "  padding-left: 16px; }\n" +
                "\n" +
                "td.large-1.last,\n" +
                "th.large-1.last {\n" +
                "  padding-right: 16px; }\n" +
                "\n" +
                ".collapse > tbody > tr > td.large-1,\n" +
                ".collapse > tbody > tr > th.large-1 {\n" +
                "  padding-right: 0;\n" +
                "  padding-left: 0;\n" +
                "  width: 48.33333px; }\n" +
                "\n" +
                ".collapse td.large-1.first,\n" +
                ".collapse th.large-1.first,\n" +
                ".collapse td.large-1.last,\n" +
                ".collapse th.large-1.last {\n" +
                "  width: 56.33333px; }\n" +
                "\n" +
                "td.large-1 center,\n" +
                "th.large-1 center {\n" +
                "  min-width: 0.33333px; }\n" +
                "\n" +
                ".body .columns td.large-1,\n" +
                ".body .column td.large-1,\n" +
                ".body .columns th.large-1,\n" +
                ".body .column th.large-1 {\n" +
                "  width: 8.33333%; }\n" +
                "\n" +
                "td.large-2,\n" +
                "th.large-2 {\n" +
                "  width: 80.66667px;\n" +
                "  padding-left: 8px;\n" +
                "  padding-right: 8px; }\n" +
                "\n" +
                "td.large-2.first,\n" +
                "th.large-2.first {\n" +
                "  padding-left: 16px; }\n" +
                "\n" +
                "td.large-2.last,\n" +
                "th.large-2.last {\n" +
                "  padding-right: 16px; }\n" +
                "\n" +
                ".collapse > tbody > tr > td.large-2,\n" +
                ".collapse > tbody > tr > th.large-2 {\n" +
                "  padding-right: 0;\n" +
                "  padding-left: 0;\n" +
                "  width: 96.66667px; }\n" +
                "\n" +
                ".collapse td.large-2.first,\n" +
                ".collapse th.large-2.first,\n" +
                ".collapse td.large-2.last,\n" +
                ".collapse th.large-2.last {\n" +
                "  width: 104.66667px; }\n" +
                "\n" +
                "td.large-2 center,\n" +
                "th.large-2 center {\n" +
                "  min-width: 48.66667px; }\n" +
                "\n" +
                ".body .columns td.large-2,\n" +
                ".body .column td.large-2,\n" +
                ".body .columns th.large-2,\n" +
                ".body .column th.large-2 {\n" +
                "  width: 16.66667%; }\n" +
                "\n" +
                "td.large-3,\n" +
                "th.large-3 {\n" +
                "  width: 129px;\n" +
                "  padding-left: 8px;\n" +
                "  padding-right: 8px; }\n" +
                "\n" +
                "td.large-3.first,\n" +
                "th.large-3.first {\n" +
                "  padding-left: 16px; }\n" +
                "\n" +
                "td.large-3.last,\n" +
                "th.large-3.last {\n" +
                "  padding-right: 16px; }\n" +
                "\n" +
                ".collapse > tbody > tr > td.large-3,\n" +
                ".collapse > tbody > tr > th.large-3 {\n" +
                "  padding-right: 0;\n" +
                "  padding-left: 0;\n" +
                "  width: 145px; }\n" +
                "\n" +
                ".collapse td.large-3.first,\n" +
                ".collapse th.large-3.first,\n" +
                ".collapse td.large-3.last,\n" +
                ".collapse th.large-3.last {\n" +
                "  width: 153px; }\n" +
                "\n" +
                "td.large-3 center,\n" +
                "th.large-3 center {\n" +
                "  min-width: 97px; }\n" +
                "\n" +
                ".body .columns td.large-3,\n" +
                ".body .column td.large-3,\n" +
                ".body .columns th.large-3,\n" +
                ".body .column th.large-3 {\n" +
                "  width: 25%; }\n" +
                "\n" +
                "td.large-4,\n" +
                "th.large-4 {\n" +
                "  width: 177.33333px;\n" +
                "  padding-left: 8px;\n" +
                "  padding-right: 8px; }\n" +
                "\n" +
                "td.large-4.first,\n" +
                "th.large-4.first {\n" +
                "  padding-left: 16px; }\n" +
                "\n" +
                "td.large-4.last,\n" +
                "th.large-4.last {\n" +
                "  padding-right: 16px; }\n" +
                "\n" +
                ".collapse > tbody > tr > td.large-4,\n" +
                ".collapse > tbody > tr > th.large-4 {\n" +
                "  padding-right: 0;\n" +
                "  padding-left: 0;\n" +
                "  width: 193.33333px; }\n" +
                "\n" +
                ".collapse td.large-4.first,\n" +
                ".collapse th.large-4.first,\n" +
                ".collapse td.large-4.last,\n" +
                ".collapse th.large-4.last {\n" +
                "  width: 201.33333px; }\n" +
                "\n" +
                "td.large-4 center,\n" +
                "th.large-4 center {\n" +
                "  min-width: 145.33333px; }\n" +
                "\n" +
                ".body .columns td.large-4,\n" +
                ".body .column td.large-4,\n" +
                ".body .columns th.large-4,\n" +
                ".body .column th.large-4 {\n" +
                "  width: 33.33333%; }\n" +
                "\n" +
                "td.large-5,\n" +
                "th.large-5 {\n" +
                "  width: 225.66667px;\n" +
                "  padding-left: 8px;\n" +
                "  padding-right: 8px; }\n" +
                "\n" +
                "td.large-5.first,\n" +
                "th.large-5.first {\n" +
                "  padding-left: 16px; }\n" +
                "\n" +
                "td.large-5.last,\n" +
                "th.large-5.last {\n" +
                "  padding-right: 16px; }\n" +
                "\n" +
                ".collapse > tbody > tr > td.large-5,\n" +
                ".collapse > tbody > tr > th.large-5 {\n" +
                "  padding-right: 0;\n" +
                "  padding-left: 0;\n" +
                "  width: 241.66667px; }\n" +
                "\n" +
                ".collapse td.large-5.first,\n" +
                ".collapse th.large-5.first,\n" +
                ".collapse td.large-5.last,\n" +
                ".collapse th.large-5.last {\n" +
                "  width: 249.66667px; }\n" +
                "\n" +
                "td.large-5 center,\n" +
                "th.large-5 center {\n" +
                "  min-width: 193.66667px; }\n" +
                "\n" +
                ".body .columns td.large-5,\n" +
                ".body .column td.large-5,\n" +
                ".body .columns th.large-5,\n" +
                ".body .column th.large-5 {\n" +
                "  width: 41.66667%; }\n" +
                "\n" +
                "td.large-6,\n" +
                "th.large-6 {\n" +
                "  width: 274px;\n" +
                "  padding-left: 8px;\n" +
                "  padding-right: 8px; }\n" +
                "\n" +
                "td.large-6.first,\n" +
                "th.large-6.first {\n" +
                "  padding-left: 16px; }\n" +
                "\n" +
                "td.large-6.last,\n" +
                "th.large-6.last {\n" +
                "  padding-right: 16px; }\n" +
                "\n" +
                ".collapse > tbody > tr > td.large-6,\n" +
                ".collapse > tbody > tr > th.large-6 {\n" +
                "  padding-right: 0;\n" +
                "  padding-left: 0;\n" +
                "  width: 290px; }\n" +
                "\n" +
                ".collapse td.large-6.first,\n" +
                ".collapse th.large-6.first,\n" +
                ".collapse td.large-6.last,\n" +
                ".collapse th.large-6.last {\n" +
                "  width: 298px; }\n" +
                "\n" +
                "td.large-6 center,\n" +
                "th.large-6 center {\n" +
                "  min-width: 242px; }\n" +
                "\n" +
                ".body .columns td.large-6,\n" +
                ".body .column td.large-6,\n" +
                ".body .columns th.large-6,\n" +
                ".body .column th.large-6 {\n" +
                "  width: 50%; }\n" +
                "\n" +
                "td.large-7,\n" +
                "th.large-7 {\n" +
                "  width: 322.33333px;\n" +
                "  padding-left: 8px;\n" +
                "  padding-right: 8px; }\n" +
                "\n" +
                "td.large-7.first,\n" +
                "th.large-7.first {\n" +
                "  padding-left: 16px; }\n" +
                "\n" +
                "td.large-7.last,\n" +
                "th.large-7.last {\n" +
                "  padding-right: 16px; }\n" +
                "\n" +
                ".collapse > tbody > tr > td.large-7,\n" +
                ".collapse > tbody > tr > th.large-7 {\n" +
                "  padding-right: 0;\n" +
                "  padding-left: 0;\n" +
                "  width: 338.33333px; }\n" +
                "\n" +
                ".collapse td.large-7.first,\n" +
                ".collapse th.large-7.first,\n" +
                ".collapse td.large-7.last,\n" +
                ".collapse th.large-7.last {\n" +
                "  width: 346.33333px; }\n" +
                "\n" +
                "td.large-7 center,\n" +
                "th.large-7 center {\n" +
                "  min-width: 290.33333px; }\n" +
                "\n" +
                ".body .columns td.large-7,\n" +
                ".body .column td.large-7,\n" +
                ".body .columns th.large-7,\n" +
                ".body .column th.large-7 {\n" +
                "  width: 58.33333%; }\n" +
                "\n" +
                "td.large-8,\n" +
                "th.large-8 {\n" +
                "  width: 370.66667px;\n" +
                "  padding-left: 8px;\n" +
                "  padding-right: 8px; }\n" +
                "\n" +
                "td.large-8.first,\n" +
                "th.large-8.first {\n" +
                "  padding-left: 16px; }\n" +
                "\n" +
                "td.large-8.last,\n" +
                "th.large-8.last {\n" +
                "  padding-right: 16px; }\n" +
                "\n" +
                ".collapse > tbody > tr > td.large-8,\n" +
                ".collapse > tbody > tr > th.large-8 {\n" +
                "  padding-right: 0;\n" +
                "  padding-left: 0;\n" +
                "  width: 386.66667px; }\n" +
                "\n" +
                ".collapse td.large-8.first,\n" +
                ".collapse th.large-8.first,\n" +
                ".collapse td.large-8.last,\n" +
                ".collapse th.large-8.last {\n" +
                "  width: 394.66667px; }\n" +
                "\n" +
                "td.large-8 center,\n" +
                "th.large-8 center {\n" +
                "  min-width: 338.66667px; }\n" +
                "\n" +
                ".body .columns td.large-8,\n" +
                ".body .column td.large-8,\n" +
                ".body .columns th.large-8,\n" +
                ".body .column th.large-8 {\n" +
                "  width: 66.66667%; }\n" +
                "\n" +
                "td.large-9,\n" +
                "th.large-9 {\n" +
                "  width: 419px;\n" +
                "  padding-left: 8px;\n" +
                "  padding-right: 8px; }\n" +
                "\n" +
                "td.large-9.first,\n" +
                "th.large-9.first {\n" +
                "  padding-left: 16px; }\n" +
                "\n" +
                "td.large-9.last,\n" +
                "th.large-9.last {\n" +
                "  padding-right: 16px; }\n" +
                "\n" +
                ".collapse > tbody > tr > td.large-9,\n" +
                ".collapse > tbody > tr > th.large-9 {\n" +
                "  padding-right: 0;\n" +
                "  padding-left: 0;\n" +
                "  width: 435px; }\n" +
                "\n" +
                ".collapse td.large-9.first,\n" +
                ".collapse th.large-9.first,\n" +
                ".collapse td.large-9.last,\n" +
                ".collapse th.large-9.last {\n" +
                "  width: 443px; }\n" +
                "\n" +
                "td.large-9 center,\n" +
                "th.large-9 center {\n" +
                "  min-width: 387px; }\n" +
                "\n" +
                ".body .columns td.large-9,\n" +
                ".body .column td.large-9,\n" +
                ".body .columns th.large-9,\n" +
                ".body .column th.large-9 {\n" +
                "  width: 75%; }\n" +
                "\n" +
                "td.large-10,\n" +
                "th.large-10 {\n" +
                "  width: 467.33333px;\n" +
                "  padding-left: 8px;\n" +
                "  padding-right: 8px; }\n" +
                "\n" +
                "td.large-10.first,\n" +
                "th.large-10.first {\n" +
                "  padding-left: 16px; }\n" +
                "\n" +
                "td.large-10.last,\n" +
                "th.large-10.last {\n" +
                "  padding-right: 16px; }\n" +
                "\n" +
                ".collapse > tbody > tr > td.large-10,\n" +
                ".collapse > tbody > tr > th.large-10 {\n" +
                "  padding-right: 0;\n" +
                "  padding-left: 0;\n" +
                "  width: 483.33333px; }\n" +
                "\n" +
                ".collapse td.large-10.first,\n" +
                ".collapse th.large-10.first,\n" +
                ".collapse td.large-10.last,\n" +
                ".collapse th.large-10.last {\n" +
                "  width: 491.33333px; }\n" +
                "\n" +
                "td.large-10 center,\n" +
                "th.large-10 center {\n" +
                "  min-width: 435.33333px; }\n" +
                "\n" +
                ".body .columns td.large-10,\n" +
                ".body .column td.large-10,\n" +
                ".body .columns th.large-10,\n" +
                ".body .column th.large-10 {\n" +
                "  width: 83.33333%; }\n" +
                "\n" +
                "td.large-11,\n" +
                "th.large-11 {\n" +
                "  width: 515.66667px;\n" +
                "  padding-left: 8px;\n" +
                "  padding-right: 8px; }\n" +
                "\n" +
                "td.large-11.first,\n" +
                "th.large-11.first {\n" +
                "  padding-left: 16px; }\n" +
                "\n" +
                "td.large-11.last,\n" +
                "th.large-11.last {\n" +
                "  padding-right: 16px; }\n" +
                "\n" +
                ".collapse > tbody > tr > td.large-11,\n" +
                ".collapse > tbody > tr > th.large-11 {\n" +
                "  padding-right: 0;\n" +
                "  padding-left: 0;\n" +
                "  width: 531.66667px; }\n" +
                "\n" +
                ".collapse td.large-11.first,\n" +
                ".collapse th.large-11.first,\n" +
                ".collapse td.large-11.last,\n" +
                ".collapse th.large-11.last {\n" +
                "  width: 539.66667px; }\n" +
                "\n" +
                "td.large-11 center,\n" +
                "th.large-11 center {\n" +
                "  min-width: 483.66667px; }\n" +
                "\n" +
                ".body .columns td.large-11,\n" +
                ".body .column td.large-11,\n" +
                ".body .columns th.large-11,\n" +
                ".body .column th.large-11 {\n" +
                "  width: 91.66667%; }\n" +
                "\n" +
                "td.large-12,\n" +
                "th.large-12 {\n" +
                "  width: 564px;\n" +
                "  padding-left: 8px;\n" +
                "  padding-right: 8px; }\n" +
                "\n" +
                "td.large-12.first,\n" +
                "th.large-12.first {\n" +
                "  padding-left: 16px; }\n" +
                "\n" +
                "td.large-12.last,\n" +
                "th.large-12.last {\n" +
                "  padding-right: 16px; }\n" +
                "\n" +
                ".collapse > tbody > tr > td.large-12,\n" +
                ".collapse > tbody > tr > th.large-12 {\n" +
                "  padding-right: 0;\n" +
                "  padding-left: 0;\n" +
                "  width: 580px; }\n" +
                "\n" +
                ".collapse td.large-12.first,\n" +
                ".collapse th.large-12.first,\n" +
                ".collapse td.large-12.last,\n" +
                ".collapse th.large-12.last {\n" +
                "  width: 588px; }\n" +
                "\n" +
                "td.large-12 center,\n" +
                "th.large-12 center {\n" +
                "  min-width: 532px; }\n" +
                "\n" +
                ".body .columns td.large-12,\n" +
                ".body .column td.large-12,\n" +
                ".body .columns th.large-12,\n" +
                ".body .column th.large-12 {\n" +
                "  width: 100%; }\n" +
                "\n" +
                "td.large-offset-1,\n" +
                "td.large-offset-1.first,\n" +
                "td.large-offset-1.last,\n" +
                "th.large-offset-1,\n" +
                "th.large-offset-1.first,\n" +
                "th.large-offset-1.last {\n" +
                "  padding-left: 64.33333px; }\n" +
                "\n" +
                "td.large-offset-2,\n" +
                "td.large-offset-2.first,\n" +
                "td.large-offset-2.last,\n" +
                "th.large-offset-2,\n" +
                "th.large-offset-2.first,\n" +
                "th.large-offset-2.last {\n" +
                "  padding-left: 112.66667px; }\n" +
                "\n" +
                "td.large-offset-3,\n" +
                "td.large-offset-3.first,\n" +
                "td.large-offset-3.last,\n" +
                "th.large-offset-3,\n" +
                "th.large-offset-3.first,\n" +
                "th.large-offset-3.last {\n" +
                "  padding-left: 161px; }\n" +
                "\n" +
                "td.large-offset-4,\n" +
                "td.large-offset-4.first,\n" +
                "td.large-offset-4.last,\n" +
                "th.large-offset-4,\n" +
                "th.large-offset-4.first,\n" +
                "th.large-offset-4.last {\n" +
                "  padding-left: 209.33333px; }\n" +
                "\n" +
                "td.large-offset-5,\n" +
                "td.large-offset-5.first,\n" +
                "td.large-offset-5.last,\n" +
                "th.large-offset-5,\n" +
                "th.large-offset-5.first,\n" +
                "th.large-offset-5.last {\n" +
                "  padding-left: 257.66667px; }\n" +
                "\n" +
                "td.large-offset-6,\n" +
                "td.large-offset-6.first,\n" +
                "td.large-offset-6.last,\n" +
                "th.large-offset-6,\n" +
                "th.large-offset-6.first,\n" +
                "th.large-offset-6.last {\n" +
                "  padding-left: 306px; }\n" +
                "\n" +
                "td.large-offset-7,\n" +
                "td.large-offset-7.first,\n" +
                "td.large-offset-7.last,\n" +
                "th.large-offset-7,\n" +
                "th.large-offset-7.first,\n" +
                "th.large-offset-7.last {\n" +
                "  padding-left: 354.33333px; }\n" +
                "\n" +
                "td.large-offset-8,\n" +
                "td.large-offset-8.first,\n" +
                "td.large-offset-8.last,\n" +
                "th.large-offset-8,\n" +
                "th.large-offset-8.first,\n" +
                "th.large-offset-8.last {\n" +
                "  padding-left: 402.66667px; }\n" +
                "\n" +
                "td.large-offset-9,\n" +
                "td.large-offset-9.first,\n" +
                "td.large-offset-9.last,\n" +
                "th.large-offset-9,\n" +
                "th.large-offset-9.first,\n" +
                "th.large-offset-9.last {\n" +
                "  padding-left: 451px; }\n" +
                "\n" +
                "td.large-offset-10,\n" +
                "td.large-offset-10.first,\n" +
                "td.large-offset-10.last,\n" +
                "th.large-offset-10,\n" +
                "th.large-offset-10.first,\n" +
                "th.large-offset-10.last {\n" +
                "  padding-left: 499.33333px; }\n" +
                "\n" +
                "td.large-offset-11,\n" +
                "td.large-offset-11.first,\n" +
                "td.large-offset-11.last,\n" +
                "th.large-offset-11,\n" +
                "th.large-offset-11.first,\n" +
                "th.large-offset-11.last {\n" +
                "  padding-left: 547.66667px; }\n" +
                "\n" +
                "td.expander,\n" +
                "th.expander {\n" +
                "  visibility: hidden;\n" +
                "  width: 0;\n" +
                "  padding: 0 !important; }\n" +
                "\n" +
                "table.container.radius {\n" +
                "  border-radius: 0;\n" +
                "  border-collapse: separate; }\n" +
                "\n" +
                ".block-grid {\n" +
                "  width: 100%;\n" +
                "  max-width: 580px; }\n" +
                "  .block-grid td {\n" +
                "    display: inline-block;\n" +
                "    padding: 8px; }\n" +
                "\n" +
                ".up-2 td {\n" +
                "  width: 274px !important; }\n" +
                "\n" +
                ".up-3 td {\n" +
                "  width: 177px !important; }\n" +
                "\n" +
                ".up-4 td {\n" +
                "  width: 129px !important; }\n" +
                "\n" +
                ".up-5 td {\n" +
                "  width: 100px !important; }\n" +
                "\n" +
                ".up-6 td {\n" +
                "  width: 80px !important; }\n" +
                "\n" +
                ".up-7 td {\n" +
                "  width: 66px !important; }\n" +
                "\n" +
                ".up-8 td {\n" +
                "  width: 56px !important; }\n" +
                "\n" +
                "table.text-center,\n" +
                "th.text-center,\n" +
                "td.text-center,\n" +
                "h1.text-center,\n" +
                "h2.text-center,\n" +
                "h3.text-center,\n" +
                "h4.text-center,\n" +
                "h5.text-center,\n" +
                "h6.text-center,\n" +
                "p.text-center,\n" +
                "span.text-center {\n" +
                "  text-align: center; }\n" +
                "\n" +
                "table.text-left,\n" +
                "th.text-left,\n" +
                "td.text-left,\n" +
                "h1.text-left,\n" +
                "h2.text-left,\n" +
                "h3.text-left,\n" +
                "h4.text-left,\n" +
                "h5.text-left,\n" +
                "h6.text-left,\n" +
                "p.text-left,\n" +
                "span.text-left {\n" +
                "  text-align: left; }\n" +
                "\n" +
                "table.text-right,\n" +
                "th.text-right,\n" +
                "td.text-right,\n" +
                "h1.text-right,\n" +
                "h2.text-right,\n" +
                "h3.text-right,\n" +
                "h4.text-right,\n" +
                "h5.text-right,\n" +
                "h6.text-right,\n" +
                "p.text-right,\n" +
                "span.text-right {\n" +
                "  text-align: right; }\n" +
                "\n" +
                "span.text-center {\n" +
                "  display: block;\n" +
                "  width: 100%;\n" +
                "  text-align: center; }\n" +
                "\n" +
                "@media only screen and (max-width: 596px) {\n" +
                "  .small-float-center {\n" +
                "    margin: 0 auto !important;\n" +
                "    float: none !important;\n" +
                "    text-align: center !important; }\n" +
                "  .small-text-center {\n" +
                "    text-align: center !important; }\n" +
                "  .small-text-left {\n" +
                "    text-align: left !important; }\n" +
                "  .small-text-right {\n" +
                "    text-align: right !important; } }\n" +
                "\n" +
                "img.float-left {\n" +
                "  float: left;\n" +
                "  text-align: left; }\n" +
                "\n" +
                "img.float-right {\n" +
                "  float: right;\n" +
                "  text-align: right; }\n" +
                "\n" +
                "img.float-center,\n" +
                "img.text-center {\n" +
                "  margin: 0 auto;\n" +
                "  Margin: 0 auto;\n" +
                "  float: none;\n" +
                "  text-align: center; }\n" +
                "\n" +
                "table.float-center,\n" +
                "td.float-center,\n" +
                "th.float-center {\n" +
                "  margin: 0 auto;\n" +
                "  Margin: 0 auto;\n" +
                "  float: none;\n" +
                "  text-align: center; }\n" +
                "\n" +
                ".hide-for-large {\n" +
                "  display: none !important;\n" +
                "  mso-hide: all;\n" +
                "  overflow: hidden;\n" +
                "  max-height: 0;\n" +
                "  font-size: 0;\n" +
                "  width: 0;\n" +
                "  line-height: 0; }\n" +
                "  @media only screen and (max-width: 596px) {\n" +
                "    .hide-for-large {\n" +
                "      display: block !important;\n" +
                "      width: auto !important;\n" +
                "      overflow: visible !important;\n" +
                "      max-height: none !important;\n" +
                "      font-size: inherit !important;\n" +
                "      line-height: inherit !important; } }\n" +
                "\n" +
                "table.body table.container .hide-for-large * {\n" +
                "  mso-hide: all; }\n" +
                "\n" +
                "@media only screen and (max-width: 596px) {\n" +
                "  table.body table.container .hide-for-large,\n" +
                "  table.body table.container .row.hide-for-large {\n" +
                "    display: table !important;\n" +
                "    width: 100% !important; } }\n" +
                "\n" +
                "@media only screen and (max-width: 596px) {\n" +
                "  table.body table.container .callout-inner.hide-for-large {\n" +
                "    display: table-cell !important;\n" +
                "    width: 100% !important; } }\n" +
                "\n" +
                "@media only screen and (max-width: 596px) {\n" +
                "  table.body table.container .show-for-large {\n" +
                "    display: none !important;\n" +
                "    width: 0;\n" +
                "    mso-hide: all;\n" +
                "    overflow: hidden; } }\n" +
                "\n" +
                "body,\n" +
                "table.body,\n" +
                "h1,\n" +
                "h2,\n" +
                "h3,\n" +
                "h4,\n" +
                "h5,\n" +
                "h6,\n" +
                "p,\n" +
                "td,\n" +
                "th,\n" +
                "a {\n" +
                "  color: #0a0a0a;\n" +
                "  font-family: Helvetica, Arial, sans-serif;\n" +
                "  font-weight: normal;\n" +
                "  padding: 0;\n" +
                "  margin: 0;\n" +
                "  Margin: 0;\n" +
                "  text-align: left;\n" +
                "  line-height: 1.3; }\n" +
                "\n" +
                "h1,\n" +
                "h2,\n" +
                "h3,\n" +
                "h4,\n" +
                "h5,\n" +
                "h6 {\n" +
                "  color: inherit;\n" +
                "  word-wrap: normal;\n" +
                "  font-family: Helvetica, Arial, sans-serif;\n" +
                "  font-weight: normal;\n" +
                "  margin-bottom: 10px;\n" +
                "  Margin-bottom: 10px; }\n" +
                "\n" +
                "h1 {\n" +
                "  font-size: 34px; }\n" +
                "\n" +
                "h2 {\n" +
                "  font-size: 30px; }\n" +
                "\n" +
                "h3 {\n" +
                "  font-size: 28px; }\n" +
                "\n" +
                "h4 {\n" +
                "  font-size: 24px; }\n" +
                "\n" +
                "h5 {\n" +
                "  font-size: 20px; }\n" +
                "\n" +
                "h6 {\n" +
                "  font-size: 18px; }\n" +
                "\n" +
                "body,\n" +
                "table.body,\n" +
                "p,\n" +
                "td,\n" +
                "th {\n" +
                "  font-size: 16px;\n" +
                "  line-height: 1.3; }\n" +
                "\n" +
                "p {\n" +
                "  margin-bottom: 10px;\n" +
                "  Margin-bottom: 10px; }\n" +
                "  p.lead {\n" +
                "    font-size: 20px;\n" +
                "    line-height: 1.6; }\n" +
                "  p.subheader {\n" +
                "    margin-top: 4px;\n" +
                "    margin-bottom: 8px;\n" +
                "    Margin-top: 4px;\n" +
                "    Margin-bottom: 8px;\n" +
                "    font-weight: normal;\n" +
                "    line-height: 1.4;\n" +
                "    color: #8a8a8a; }\n" +
                "\n" +
                "small {\n" +
                "  font-size: 80%;\n" +
                "  color: #cacaca; }\n" +
                "\n" +
                "a {\n" +
                "  color: #2199e8;\n" +
                "  text-decoration: none; }\n" +
                "  a:hover {\n" +
                "    color: #147dc2; }\n" +
                "  a:active {\n" +
                "    color: #147dc2; }\n" +
                "  a:visited {\n" +
                "    color: #2199e8; }\n" +
                "\n" +
                "h1 a,\n" +
                "h1 a:visited,\n" +
                "h2 a,\n" +
                "h2 a:visited,\n" +
                "h3 a,\n" +
                "h3 a:visited,\n" +
                "h4 a,\n" +
                "h4 a:visited,\n" +
                "h5 a,\n" +
                "h5 a:visited,\n" +
                "h6 a,\n" +
                "h6 a:visited {\n" +
                "  color: #2199e8; }\n" +
                "\n" +
                "pre {\n" +
                "  background: #f3f3f3;\n" +
                "  margin: 30px 0;\n" +
                "  Margin: 30px 0; }\n" +
                "  pre code {\n" +
                "    color: #cacaca; }\n" +
                "    pre code span.callout {\n" +
                "      color: #8a8a8a;\n" +
                "      font-weight: bold; }\n" +
                "    pre code span.callout-strong {\n" +
                "      color: #ff6908;\n" +
                "      font-weight: bold; }\n" +
                "\n" +
                "table.hr {\n" +
                "  width: 100%; }\n" +
                "  table.hr th {\n" +
                "    height: 0;\n" +
                "    max-width: 580px;\n" +
                "    border-top: 0;\n" +
                "    border-right: 0;\n" +
                "    border-bottom: 1px solid #0a0a0a;\n" +
                "    border-left: 0;\n" +
                "    margin: 20px auto;\n" +
                "    Margin: 20px auto;\n" +
                "    clear: both; }\n" +
                "\n" +
                ".stat {\n" +
                "  font-size: 40px;\n" +
                "  line-height: 1; }\n" +
                "  p + .stat {\n" +
                "    margin-top: -16px;\n" +
                "    Margin-top: -16px; }\n" +
                "\n" +
                "span.preheader {\n" +
                "  display: none !important;\n" +
                "  visibility: hidden;\n" +
                "  mso-hide: all !important;\n" +
                "  font-size: 1px;\n" +
                "  color: #f3f3f3;\n" +
                "  line-height: 1px;\n" +
                "  max-height: 0px;\n" +
                "  max-width: 0px;\n" +
                "  opacity: 0;\n" +
                "  overflow: hidden; }\n" +
                "\n" +
                "table.button {\n" +
                "  width: auto;\n" +
                "  margin: 0 0 16px 0;\n" +
                "  Margin: 0 0 16px 0; }\n" +
                "  table.button table td {\n" +
                "    text-align: left;\n" +
                "    color: #fefefe;\n" +
                "    background: #2199e8;\n" +
                "    border: 2px solid #2199e8; }\n" +
                "    table.button table td a {\n" +
                "      font-family: Helvetica, Arial, sans-serif;\n" +
                "      font-size: 16px;\n" +
                "      font-weight: bold;\n" +
                "      color: #fefefe;\n" +
                "      text-decoration: none;\n" +
                "      display: inline-block;\n" +
                "      padding: 8px 16px 8px 16px;\n" +
                "      border: 0 solid #2199e8;\n" +
                "      border-radius: 3px; }\n" +
                "  table.button.radius table td {\n" +
                "    border-radius: 3px;\n" +
                "    border: none; }\n" +
                "  table.button.rounded table td {\n" +
                "    border-radius: 500px;\n" +
                "    border: none; }\n" +
                "\n" +
                "table.button:hover table tr td a,\n" +
                "table.button:active table tr td a,\n" +
                "table.button table tr td a:visited,\n" +
                "table.button.tiny:hover table tr td a,\n" +
                "table.button.tiny:active table tr td a,\n" +
                "table.button.tiny table tr td a:visited,\n" +
                "table.button.small:hover table tr td a,\n" +
                "table.button.small:active table tr td a,\n" +
                "table.button.small table tr td a:visited,\n" +
                "table.button.large:hover table tr td a,\n" +
                "table.button.large:active table tr td a,\n" +
                "table.button.large table tr td a:visited {\n" +
                "  color: #fefefe; }\n" +
                "\n" +
                "table.button.tiny table td,\n" +
                "table.button.tiny table a {\n" +
                "  padding: 4px 8px 4px 8px; }\n" +
                "\n" +
                "table.button.tiny table a {\n" +
                "  font-size: 10px;\n" +
                "  font-weight: normal; }\n" +
                "\n" +
                "table.button.small table td,\n" +
                "table.button.small table a {\n" +
                "  padding: 5px 10px 5px 10px;\n" +
                "  font-size: 12px; }\n" +
                "\n" +
                "table.button.large table a {\n" +
                "  padding: 10px 20px 10px 20px;\n" +
                "  font-size: 20px; }\n" +
                "\n" +
                "table.button.expand,\n" +
                "table.button.expanded {\n" +
                "  width: 100% !important; }\n" +
                "  table.button.expand table,\n" +
                "  table.button.expanded table {\n" +
                "    width: 100%; }\n" +
                "    table.button.expand table a,\n" +
                "    table.button.expanded table a {\n" +
                "      text-align: center;\n" +
                "      width: 100%;\n" +
                "      padding-left: 0;\n" +
                "      padding-right: 0; }\n" +
                "  table.button.expand center,\n" +
                "  table.button.expanded center {\n" +
                "    min-width: 0; }\n" +
                "\n" +
                "table.button:hover table td,\n" +
                "table.button:visited table td,\n" +
                "table.button:active table td {\n" +
                "  background: #147dc2;\n" +
                "  color: #fefefe; }\n" +
                "\n" +
                "table.button:hover table a,\n" +
                "table.button:visited table a,\n" +
                "table.button:active table a {\n" +
                "  border: 0 solid #147dc2; }\n" +
                "\n" +
                "table.button.secondary table td {\n" +
                "  background: #777777;\n" +
                "  color: #fefefe;\n" +
                "  border: 0px solid #777777; }\n" +
                "\n" +
                "table.button.secondary table a {\n" +
                "  color: #fefefe;\n" +
                "  border: 0 solid #777777; }\n" +
                "\n" +
                "table.button.secondary:hover table td {\n" +
                "  background: #919191;\n" +
                "  color: #fefefe; }\n" +
                "\n" +
                "table.button.secondary:hover table a {\n" +
                "  border: 0 solid #919191; }\n" +
                "\n" +
                "table.button.secondary:hover table td a {\n" +
                "  color: #fefefe; }\n" +
                "\n" +
                "table.button.secondary:active table td a {\n" +
                "  color: #fefefe; }\n" +
                "\n" +
                "table.button.secondary table td a:visited {\n" +
                "  color: #fefefe; }\n" +
                "\n" +
                "table.button.success table td {\n" +
                "  background: #3adb76;\n" +
                "  border: 0px solid #3adb76; }\n" +
                "\n" +
                "table.button.success table a {\n" +
                "  border: 0 solid #3adb76; }\n" +
                "\n" +
                "table.button.success:hover table td {\n" +
                "  background: #23bf5d; }\n" +
                "\n" +
                "table.button.success:hover table a {\n" +
                "  border: 0 solid #23bf5d; }\n" +
                "\n" +
                "table.button.alert table td {\n" +
                "  background: #ec5840;\n" +
                "  border: 0px solid #ec5840; }\n" +
                "\n" +
                "table.button.alert table a {\n" +
                "  border: 0 solid #ec5840; }\n" +
                "\n" +
                "table.button.alert:hover table td {\n" +
                "  background: #e23317; }\n" +
                "\n" +
                "table.button.alert:hover table a {\n" +
                "  border: 0 solid #e23317; }\n" +
                "\n" +
                "table.button.warning table td {\n" +
                "  background: #ffae00;\n" +
                "  border: 0px solid #ffae00; }\n" +
                "\n" +
                "table.button.warning table a {\n" +
                "  border: 0px solid #ffae00; }\n" +
                "\n" +
                "table.button.warning:hover table td {\n" +
                "  background: #cc8b00; }\n" +
                "\n" +
                "table.button.warning:hover table a {\n" +
                "  border: 0px solid #cc8b00; }\n" +
                "\n" +
                "table.callout {\n" +
                "  margin-bottom: 16px;\n" +
                "  Margin-bottom: 16px; }\n" +
                "\n" +
                "th.callout-inner {\n" +
                "  width: 100%;\n" +
                "  border: 1px solid #cbcbcb;\n" +
                "  padding: 10px;\n" +
                "  background: #fefefe; }\n" +
                "  th.callout-inner.primary {\n" +
                "    background: #def0fc;\n" +
                "    border: 1px solid #444444;\n" +
                "    color: #0a0a0a; }\n" +
                "  th.callout-inner.secondary {\n" +
                "    background: #ebebeb;\n" +
                "    border: 1px solid #444444;\n" +
                "    color: #0a0a0a; }\n" +
                "  th.callout-inner.success {\n" +
                "    background: #e1faea;\n" +
                "    border: 1px solid #1b9448;\n" +
                "    color: #fefefe; }\n" +
                "  th.callout-inner.warning {\n" +
                "    background: #fff3d9;\n" +
                "    border: 1px solid #996800;\n" +
                "    color: #fefefe; }\n" +
                "  th.callout-inner.alert {\n" +
                "    background: #fce6e2;\n" +
                "    border: 1px solid #b42912;\n" +
                "    color: #fefefe; }\n" +
                "\n" +
                ".thumbnail {\n" +
                "  border: solid 4px #fefefe;\n" +
                "  box-shadow: 0 0 0 1px rgba(10, 10, 10, 0.2);\n" +
                "  display: inline-block;\n" +
                "  line-height: 0;\n" +
                "  max-width: 100%;\n" +
                "  transition: box-shadow 200ms ease-out;\n" +
                "  border-radius: 3px;\n" +
                "  margin-bottom: 16px; }\n" +
                "  .thumbnail:hover, .thumbnail:focus {\n" +
                "    box-shadow: 0 0 6px 1px rgba(33, 153, 232, 0.5); }\n" +
                "\n" +
                "table.menu {\n" +
                "  width: 580px; }\n" +
                "  table.menu td.menu-item,\n" +
                "  table.menu th.menu-item {\n" +
                "    padding: 10px;\n" +
                "    padding-right: 10px; }\n" +
                "    table.menu td.menu-item a,\n" +
                "    table.menu th.menu-item a {\n" +
                "      color: #2199e8; }\n" +
                "\n" +
                "table.menu.vertical td.menu-item,\n" +
                "table.menu.vertical th.menu-item {\n" +
                "  padding: 10px;\n" +
                "  padding-right: 0;\n" +
                "  display: block; }\n" +
                "  table.menu.vertical td.menu-item a,\n" +
                "  table.menu.vertical th.menu-item a {\n" +
                "    width: 100%; }\n" +
                "\n" +
                "table.menu.vertical td.menu-item table.menu.vertical td.menu-item,\n" +
                "table.menu.vertical td.menu-item table.menu.vertical th.menu-item,\n" +
                "table.menu.vertical th.menu-item table.menu.vertical td.menu-item,\n" +
                "table.menu.vertical th.menu-item table.menu.vertical th.menu-item {\n" +
                "  padding-left: 10px; }\n" +
                "\n" +
                "table.menu.text-center a {\n" +
                "  text-align: center; }\n" +
                "\n" +
                ".menu[align=\"center\"] {\n" +
                "  width: auto !important; }\n" +
                "\n" +
                "body.outlook p {\n" +
                "  display: inline !important; }\n" +
                "\n" +
                "@media only screen and (max-width: 596px) {\n" +
                "  table.body img {\n" +
                "    width: auto;\n" +
                "    height: auto; }\n" +
                "  table.body center {\n" +
                "    min-width: 0 !important; }\n" +
                "  table.body .container {\n" +
                "    width: 95% !important; }\n" +
                "  table.body .columns,\n" +
                "  table.body .column {\n" +
                "    height: auto !important;\n" +
                "    -moz-box-sizing: border-box;\n" +
                "    -webkit-box-sizing: border-box;\n" +
                "    box-sizing: border-box;\n" +
                "    padding-left: 16px !important;\n" +
                "    padding-right: 16px !important; }\n" +
                "    table.body .columns .column,\n" +
                "    table.body .columns .columns,\n" +
                "    table.body .column .column,\n" +
                "    table.body .column .columns {\n" +
                "      padding-left: 0 !important;\n" +
                "      padding-right: 0 !important; }\n" +
                "  table.body .collapse .columns,\n" +
                "  table.body .collapse .column {\n" +
                "    padding-left: 0 !important;\n" +
                "    padding-right: 0 !important; }\n" +
                "  td.small-1,\n" +
                "  th.small-1 {\n" +
                "    display: inline-block !important;\n" +
                "    width: 8.33333% !important; }\n" +
                "  td.small-2,\n" +
                "  th.small-2 {\n" +
                "    display: inline-block !important;\n" +
                "    width: 16.66667% !important; }\n" +
                "  td.small-3,\n" +
                "  th.small-3 {\n" +
                "    display: inline-block !important;\n" +
                "    width: 25% !important; }\n" +
                "  td.small-4,\n" +
                "  th.small-4 {\n" +
                "    display: inline-block !important;\n" +
                "    width: 33.33333% !important; }\n" +
                "  td.small-5,\n" +
                "  th.small-5 {\n" +
                "    display: inline-block !important;\n" +
                "    width: 41.66667% !important; }\n" +
                "  td.small-6,\n" +
                "  th.small-6 {\n" +
                "    display: inline-block !important;\n" +
                "    width: 50% !important; }\n" +
                "  td.small-7,\n" +
                "  th.small-7 {\n" +
                "    display: inline-block !important;\n" +
                "    width: 58.33333% !important; }\n" +
                "  td.small-8,\n" +
                "  th.small-8 {\n" +
                "    display: inline-block !important;\n" +
                "    width: 66.66667% !important; }\n" +
                "  td.small-9,\n" +
                "  th.small-9 {\n" +
                "    display: inline-block !important;\n" +
                "    width: 75% !important; }\n" +
                "  td.small-10,\n" +
                "  th.small-10 {\n" +
                "    display: inline-block !important;\n" +
                "    width: 83.33333% !important; }\n" +
                "  td.small-11,\n" +
                "  th.small-11 {\n" +
                "    display: inline-block !important;\n" +
                "    width: 91.66667% !important; }\n" +
                "  td.small-12,\n" +
                "  th.small-12 {\n" +
                "    display: inline-block !important;\n" +
                "    width: 100% !important; }\n" +
                "  .columns td.small-12,\n" +
                "  .column td.small-12,\n" +
                "  .columns th.small-12,\n" +
                "  .column th.small-12 {\n" +
                "    display: block !important;\n" +
                "    width: 100% !important; }\n" +
                "  table.body td.small-offset-1,\n" +
                "  table.body th.small-offset-1 {\n" +
                "    margin-left: 8.33333% !important;\n" +
                "    Margin-left: 8.33333% !important; }\n" +
                "  table.body td.small-offset-2,\n" +
                "  table.body th.small-offset-2 {\n" +
                "    margin-left: 16.66667% !important;\n" +
                "    Margin-left: 16.66667% !important; }\n" +
                "  table.body td.small-offset-3,\n" +
                "  table.body th.small-offset-3 {\n" +
                "    margin-left: 25% !important;\n" +
                "    Margin-left: 25% !important; }\n" +
                "  table.body td.small-offset-4,\n" +
                "  table.body th.small-offset-4 {\n" +
                "    margin-left: 33.33333% !important;\n" +
                "    Margin-left: 33.33333% !important; }\n" +
                "  table.body td.small-offset-5,\n" +
                "  table.body th.small-offset-5 {\n" +
                "    margin-left: 41.66667% !important;\n" +
                "    Margin-left: 41.66667% !important; }\n" +
                "  table.body td.small-offset-6,\n" +
                "  table.body th.small-offset-6 {\n" +
                "    margin-left: 50% !important;\n" +
                "    Margin-left: 50% !important; }\n" +
                "  table.body td.small-offset-7,\n" +
                "  table.body th.small-offset-7 {\n" +
                "    margin-left: 58.33333% !important;\n" +
                "    Margin-left: 58.33333% !important; }\n" +
                "  table.body td.small-offset-8,\n" +
                "  table.body th.small-offset-8 {\n" +
                "    margin-left: 66.66667% !important;\n" +
                "    Margin-left: 66.66667% !important; }\n" +
                "  table.body td.small-offset-9,\n" +
                "  table.body th.small-offset-9 {\n" +
                "    margin-left: 75% !important;\n" +
                "    Margin-left: 75% !important; }\n" +
                "  table.body td.small-offset-10,\n" +
                "  table.body th.small-offset-10 {\n" +
                "    margin-left: 83.33333% !important;\n" +
                "    Margin-left: 83.33333% !important; }\n" +
                "  table.body td.small-offset-11,\n" +
                "  table.body th.small-offset-11 {\n" +
                "    margin-left: 91.66667% !important;\n" +
                "    Margin-left: 91.66667% !important; }\n" +
                "  table.body table.columns td.expander,\n" +
                "  table.body table.columns th.expander {\n" +
                "    display: none !important; }\n" +
                "  table.body .right-text-pad,\n" +
                "  table.body .text-pad-right {\n" +
                "    padding-left: 10px !important; }\n" +
                "  table.body .left-text-pad,\n" +
                "  table.body .text-pad-left {\n" +
                "    padding-right: 10px !important; }\n" +
                "  table.menu {\n" +
                "    width: 100% !important; }\n" +
                "    table.menu td,\n" +
                "    table.menu th {\n" +
                "      width: auto !important;\n" +
                "      display: inline-block !important; }\n" +
                "    table.menu.vertical td,\n" +
                "    table.menu.vertical th, table.menu.small-vertical td,\n" +
                "    table.menu.small-vertical th {\n" +
                "      display: block !important; }\n" +
                "  table.menu[align=\"center\"] {\n" +
                "    width: auto !important; }\n" +
                "  table.button.small-expand,\n" +
                "  table.button.small-expanded {\n" +
                "    width: 100% !important; }\n" +
                "    table.button.small-expand table,\n" +
                "    table.button.small-expanded table {\n" +
                "      width: 100%; }\n" +
                "      table.button.small-expand table a,\n" +
                "      table.button.small-expanded table a {\n" +
                "        text-align: center !important;\n" +
                "        width: 100% !important;\n" +
                "        padding-left: 0 !important;\n" +
                "        padding-right: 0 !important; }\n" +
                "    table.button.small-expand center,\n" +
                "    table.button.small-expanded center {\n" +
                "      min-width: 0; } }\n";
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
    protected void end() throws IOException {
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
                .startTag("", "img").attribute("", "src", "http://dietdiaryapp.canyapan.com/email/qrcode.jpg").attribute("", "class", "float-right").endTag("", "img")
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
                .attribute("", "style", "font-size : 16px; line-height : 16px;")
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

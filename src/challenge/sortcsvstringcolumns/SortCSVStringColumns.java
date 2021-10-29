package challenge.sortcsvstringcolumns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SortCSVStringColumns {

  public static String sortCsvColumns(String csv_data) {
    String[] rows = csv_data.split("\n");
    String[] headers = rows[0].split(",");
    SortedMap<String, List<String>> sortedTableData = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    Arrays.stream(headers).forEach(header -> {
      sortedTableData.put(header, new ArrayList<>());
    });
    for (int i = 1; i < rows.length; i++) {
      String[] row = rows[i].split(",");
      for (int j = 0; j < headers.length; j++) {
        String header = headers[j];
        String rowData = j < row.length ? row[j] : "";
        sortedTableData.get(header).add(rowData);
      }
    }

    String orderedCSV = String.join(",", sortedTableData.keySet()) + "\n";

    for (int i = 0; i < rows.length - 1; i++) {
      final int rowIndex = i;
      orderedCSV += sortedTableData.entrySet().stream().map(entry -> {
        return entry.getValue().get(rowIndex);
      }).collect(Collectors.joining(","));

      if (i < rows.length - 2) {
        orderedCSV += "\n";
      }
    }

    return orderedCSV;
  }

  public static void main(String[] args) {
    String rawData = "Beth,Charles,Danielle,Adam,Eric\n" //
        + "17945,10091,10088,3907,10132\n" //
        + "2,12,13,48,11";
    System.out.println(sortCsvColumns(rawData));
  }
}

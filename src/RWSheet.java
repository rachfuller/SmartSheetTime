import com.smartsheet.api.Smartsheet;
import com.smartsheet.api.SmartsheetException;
import com.smartsheet.api.SmartsheetFactory;
import com.smartsheet.api.models.Cell;
import com.smartsheet.api.models.Column;
import com.smartsheet.api.models.Row;
import com.smartsheet.api.models.Sheet;

import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.*;

public class RWSheet 
{
	static 
	{
		//These lines enable logging to the console
		System.setProperty("Smartsheet.trace.parts", "RequestBodySummary,ResponseBodySummary");
		System.setProperty("Smartsheet.trace.pretty", "true");
	}

	//The Smartsheet API identifies columns by Id, but it's more convenient to refer to column names
	private static HashMap<String, Long> columnMap = new HashMap<String, Long>();  

	public static void main(final String[] args) 
	{
		try {
			try {
				Class.forName("com.mysql.cj.jdbc.Driver");

				//Establishing Connection to database
				Connection con = DriverManager.getConnection(
						"jdbc:mysql://159.89.42.80/timesheet_TimeKeepers", "timesheet_vergan01", "B5YJNBWRR&_&");

				if (con != null) { //if connection does exist
					Statement s = con.createStatement(); //create a new Statement object for execution of SQL queries
					ResultSet results = s.executeQuery("Select * FROM Sheets;"); //execute SQL query to get every row from database
					ArrayList<String[]> sheets = new ArrayList<>();

					while(results.next()) { //extract everything from columns from the rows extracted from the database
						String[] set = new String[3];
						set[0] = results.getString(1);
						set[1] = results.getString(2);
						set[2] = results.getString(3);
						sheets.add(set);
					}
					//create client in order to access Sheet and generate reminders later
					Smartsheet smartsheet = SmartsheetFactory.createDefaultClient();

					for(String[] set : sheets) {
						smartsheet.setAccessToken(set[1]); //get the access token that was taken from the database
						ArrayList<Timekeeper> users = new ArrayList<Timekeeper>();

						//change string to the id of the sheet you want to read
						long id = Long.parseLong(set[0]); 
						//grab the Sheet that needs reminders generated for
						Sheet reminder = smartsheet.sheetResources().getSheet(id, null, null, null, null, null, null, null, null, null);

						//put the columns in a hashmap for easy acesss
						for (Column column : reminder.getColumns()) {
							columnMap.put(column.getTitle(), column.getId());
						}

						//get the rows from the Sheet
						List<Row> rows = reminder.getRows();
						ArrayList<Row> re = new ArrayList<>();

						for(Row r : rows) {
							//grab the status cells from the corresponding status column
							Cell cell = getCellByColumnName(r, "Status");
							Object value = cell.getValue(); //get the actual value from that cell

							String val = (String) value;
							//check and see if the status is in progress
							//if it isn't, or it is null, then no need to generate reminders
							if(val != null && val.equals("In Progress")) {
								re.add(r);

								Cell periodCell = getCellByColumnName(r, "Reminder Frequency");
								double period = (Double) periodCell.getValue();

								Cell startCell = getCellByColumnName(r, "Assigned On");
								String startStr = (String) startCell.getValue();
								LocalDate start = LocalDate.parse(startStr); //turn it into a date usable by Java

								Cell categoryCell = getCellByColumnName(r, "Category");
								String category = (String) categoryCell.getValue();

								Cell itemCell = getCellByColumnName(r, "Follow-Up Item");
								String item = (String) itemCell.getValue();

								Cell assignedToCell = getCellByColumnName(r, "Assigned To");
								String assignedTo = (String) assignedToCell.getValue();

								Cell remindersCell = getCellByColumnName(r, "Reminders Sent");

								double reminders = 0;
								//if reminders need to be generated, store that value
								if(remindersCell.getValue() != null) {
									reminders = (Double) remindersCell.getValue();
								}

								//create a new Timekeeper object and feed it the parameters specified above
								Timekeeper current = new Timekeeper(period, start, category, item, assignedTo, "In Progress", r.getId(), id, reminders, set[1], set[2]);
								users.add(current);		
							}
						}
						//update the total reminders cell and generate email if needed
						updateAllTimekeepers(users);
					}
				}     
				con.close(); //close the connection
			}
			catch(Exception e) {
				System.out.println(e);
			}
		} 
		catch (Exception ex) {
			System.out.println("Exception : " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	public static void updateAllTimekeepers(ArrayList<Timekeeper> al) 
	{
		for(Timekeeper tk : al) {
			boolean check = tk.checkTime(); //check the time and see if reminder needs to be generated
			if(check == true) { //if so, generate reminder and send out email, update total reminder cell
				tk.totalReminder++;
				updateCells(tk);
				sendEmail.reminder(tk);
			}
		}
	}

	//Helper function to find cell in a row
	static Cell getCellByColumnName(Row row, String columnName)
	{
		Long colId = columnMap.get(columnName); //retrieve the column id from the hash map

		return row.getCells().stream()
				.filter(cell -> colId.equals((Long) cell.getColumnId()))
				.findFirst()
				.orElse(null);
	}

	public static void updateCells(Timekeeper tk) 
	{
		//create client in order to access Sheet and generate reminders later
		Smartsheet smartsheet = SmartsheetFactory.createDefaultClient();

		smartsheet.setAccessToken(tk.userToken); //get the access token from the timekeeper object

		try {
			Row r = smartsheet.sheetResources().rowResources().getRow(tk.sheetId, tk.rowId, null, null); //get the row from the Sheet
			Cell remindersCell = getCellByColumnName(r, "Reminders Sent"); 
			remindersCell.setValue(tk.totalReminder); //update the reminder cell with the current total reminders
			List<Cell> cellsToUpdate = Arrays.asList(remindersCell); //add that new cell to a list to be passed to Smartsheet later

			Row rowToUpdate = new Row();
			rowToUpdate.setId(tk.rowId);
			rowToUpdate.setCells(cellsToUpdate); //create a new row list to align with Smartsheet API

			//update the rows accordingly
			smartsheet.sheetResources().rowResources().updateRows(tk.sheetId, Arrays.asList(rowToUpdate));
		} 
		catch (SmartsheetException e) {
			e.printStackTrace();
		}
	}
}
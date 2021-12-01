import java.time.*;
import java.time.temporal.ChronoUnit;

public class Timekeeper 
{
	double period; //period of time elapsed before reminder needs to be sent out
	LocalDate start; //the start date of task
	String category; //category column
	long sheetId;
	String item; //follow-up item column
	String assignedTo; //assigned to column
	String status; //status column
	long rowId; //id of the row
	double totalReminder; //total reminders that have been sent column
	String userToken; //the user's Smartsheet token
	String refresh; //user's correpsonding refersh token
	
	public Timekeeper(double period, LocalDate start, String category, String item,
			String assignedTo, String status, long rowId, long sheetId, double totalReminder,
			String userToken, String refreshToken) 
	{
		this.period = period;
		this.start = start;
		this.category = category;
		this.item = item;
		this.assignedTo = assignedTo;
		this.status = status;
		this.sheetId = sheetId;
		this.rowId = rowId;
		this.totalReminder = totalReminder;
		this.userToken = userToken;
		refresh = refreshToken;
	}
	
	//check the period of time that has elapsed and see if a reminder needs to be sent
	public boolean checkTime() 
	{
		LocalDate cur = LocalDate.now(); //get current time
		long val = ChronoUnit.DAYS.between(start, cur); //get the value between the start time and the current time
		
		if (val % period == 0) { //if the required time has elapsed based on what is set in the Smartsheet
								//then we know a reminder needs to be sent
			return true;
		}
		
		return false;
	}
}
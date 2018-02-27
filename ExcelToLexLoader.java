package com.accl.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.parser.ParseException;

import com.accl.model.LexFunctionsModel;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidentity.model.ResourceConflictException;
import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuilding;
import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuildingClientBuilder;
import com.amazonaws.services.lexmodelbuilding.model.BadRequestException;
import com.amazonaws.services.lexmodelbuilding.model.ContentType;
import com.amazonaws.services.lexmodelbuilding.model.EnumerationValue;
import com.amazonaws.services.lexmodelbuilding.model.FulfillmentActivity;
import com.amazonaws.services.lexmodelbuilding.model.FulfillmentActivityType;
import com.amazonaws.services.lexmodelbuilding.model.Intent;
import com.amazonaws.services.lexmodelbuilding.model.Message;
import com.amazonaws.services.lexmodelbuilding.model.Prompt;
import com.amazonaws.services.lexmodelbuilding.model.PutBotRequest;
import com.amazonaws.services.lexmodelbuilding.model.PutBotResult;
import com.amazonaws.services.lexmodelbuilding.model.PutIntentRequest;
import com.amazonaws.services.lexmodelbuilding.model.PutIntentResult;
import com.amazonaws.services.lexmodelbuilding.model.PutSlotTypeRequest;
import com.amazonaws.services.lexmodelbuilding.model.PutSlotTypeResult;
import com.amazonaws.services.lexmodelbuilding.model.ResourceInUseException;
import com.amazonaws.services.lexmodelbuilding.model.ResourceReference;
import com.amazonaws.services.lexmodelbuilding.model.Slot;
import com.amazonaws.services.lexmodelbuilding.model.SlotConstraint;
import com.amazonaws.services.lexmodelbuilding.model.Statement;

public class ExcelToLexLoader extends LexFunctionsModel {

	public boolean checkLexActions(XSSFWorkbook workbook, String sheetName) throws ParseException {
		System.out.println("Enter into checkLexActions()");
		String BotName = System.getenv("clientId");
		System.out.println("Default bot name: " + BotName);
		// BOTNAME AND ALIAS ARE SAME
		String BotAlias = System.getenv("clientId").toUpperCase();
		System.out.println("Default bot alias: " + BotAlias);
		Map<String, List<String>> Mainslotmap = new HashMap<String, List<String>>();
		PutSlotTypeResult SlotCreation = null;
		ArrayList<Intent> intentCollection = null;
		if (sheetName.equalsIgnoreCase("Slots")) {
			System.out.println("Creating slots");
			Mainslotmap = LoadExcel(workbook, sheetName);
			if (Mainslotmap.isEmpty())
				System.out.println("no slots");
			else
				SlotCreation = createSlots(Mainslotmap, BotName);
		}
		if (sheetName.equalsIgnoreCase("SampleQuestions")) {
			System.out.println("Creating sample questions");
			Mainslotmap = LoadExcel(workbook, sheetName);
			if (Mainslotmap.isEmpty())
				System.out.println("no intents");
			else {
				intentCollection = createIntents(Mainslotmap, BotName);
				if (intentCollection.size() > 0) {
					boolean BotCreation = CreateBot(intentCollection, BotName, BotAlias);
					if (!BotCreation) {
						System.out.println("Bot is not created");
						return false;
					} else
						createBotAlias(BotName, BotAlias);
					System.out.println("Bot created with bot alias");
					return true;
				}
			}

		}
		return true;
	}

	private Map<String, List<String>> LoadExcel(XSSFWorkbook workbook, String sheetName) {
		Map<String, List<String>> slotmap = new HashMap<String, List<String>>();

		XSSFSheet sheet1 = workbook.getSheet(sheetName);
		Iterator<Row> Rowiterator = sheet1.iterator();
		while (Rowiterator.hasNext()) {

			Row nextRow = Rowiterator.next();

			int row_id = nextRow.getRowNum();

			List<String> mapvalue = null;
			if (row_id > 0) {

				if (slotmap.containsKey(nextRow.getCell(0).toString())) {
					mapvalue = new ArrayList<String>();
					mapvalue.addAll(slotmap.get(nextRow.getCell(0).toString()));
					mapvalue.add(nextRow.getCell(1).toString());
					slotmap.put(nextRow.getCell(0).toString(), mapvalue);
				} else {
					mapvalue = new ArrayList<String>();
					mapvalue.add(nextRow.getCell(1).toString());
					slotmap.put(nextRow.getCell(0).toString(), mapvalue);
				}

			}

		}
		return slotmap;
	}

	public ArrayList<String> getSlotType(List<String> value) {
		ArrayList<String> slot = new ArrayList<String>();

		String regex = "\\{(.*?)\\}";

		for (int k = 0; k < value.size(); k++) {

			Matcher m = Pattern.compile(regex).matcher(value.get(k));
			while (m.find()) {
				slot.add(m.group(1));
			}
		}
		Set<String> slotSet = new HashSet<String>(slot);
		ArrayList<String> slotlist = new ArrayList<String>(slotSet);
	    System.out.println("slotset is" + slotSet);
		return slotlist;
	}

}

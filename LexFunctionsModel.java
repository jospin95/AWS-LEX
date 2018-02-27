package com.accl.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.accl.loader.ACCLExcelLoader;
import com.accl.loader.ExcelToLexLoader;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidentity.model.ResourceConflictException;
import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuilding;
import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuildingClientBuilder;
import com.amazonaws.services.lexmodelbuilding.model.BadRequestException;
import com.amazonaws.services.lexmodelbuilding.model.BotAliasMetadata;
import com.amazonaws.services.lexmodelbuilding.model.ConflictException;
import com.amazonaws.services.lexmodelbuilding.model.ContentType;
import com.amazonaws.services.lexmodelbuilding.model.DeleteBotAliasRequest;
import com.amazonaws.services.lexmodelbuilding.model.DeleteBotRequest;
import com.amazonaws.services.lexmodelbuilding.model.DeleteIntentRequest;
import com.amazonaws.services.lexmodelbuilding.model.DeleteSlotTypeRequest;
import com.amazonaws.services.lexmodelbuilding.model.DeleteSlotTypeResult;
import com.amazonaws.services.lexmodelbuilding.model.EnumerationValue;
import com.amazonaws.services.lexmodelbuilding.model.FulfillmentActivity;
import com.amazonaws.services.lexmodelbuilding.model.FulfillmentActivityType;
import com.amazonaws.services.lexmodelbuilding.model.GetBotAliasRequest;
import com.amazonaws.services.lexmodelbuilding.model.GetBotAliasResult;
import com.amazonaws.services.lexmodelbuilding.model.GetBotAliasesRequest;
import com.amazonaws.services.lexmodelbuilding.model.GetBotAliasesResult;
import com.amazonaws.services.lexmodelbuilding.model.GetBotRequest;
import com.amazonaws.services.lexmodelbuilding.model.GetBotResult;
import com.amazonaws.services.lexmodelbuilding.model.GetBotsRequest;
import com.amazonaws.services.lexmodelbuilding.model.GetBotsResult;
import com.amazonaws.services.lexmodelbuilding.model.GetIntentRequest;
import com.amazonaws.services.lexmodelbuilding.model.GetIntentResult;
import com.amazonaws.services.lexmodelbuilding.model.GetIntentsRequest;
import com.amazonaws.services.lexmodelbuilding.model.GetIntentsResult;
import com.amazonaws.services.lexmodelbuilding.model.GetSlotTypeRequest;
import com.amazonaws.services.lexmodelbuilding.model.GetSlotTypeResult;
import com.amazonaws.services.lexmodelbuilding.model.GetSlotTypesRequest;
import com.amazonaws.services.lexmodelbuilding.model.GetSlotTypesResult;
import com.amazonaws.services.lexmodelbuilding.model.Intent;
import com.amazonaws.services.lexmodelbuilding.model.InternalFailureException;
import com.amazonaws.services.lexmodelbuilding.model.LimitExceededException;
import com.amazonaws.services.lexmodelbuilding.model.Message;
import com.amazonaws.services.lexmodelbuilding.model.PreconditionFailedException;
import com.amazonaws.services.lexmodelbuilding.model.ProcessBehavior;
import com.amazonaws.services.lexmodelbuilding.model.Prompt;
import com.amazonaws.services.lexmodelbuilding.model.PutBotAliasRequest;
import com.amazonaws.services.lexmodelbuilding.model.PutBotAliasResult;
import com.amazonaws.services.lexmodelbuilding.model.PutBotRequest;
import com.amazonaws.services.lexmodelbuilding.model.PutBotResult;
import com.amazonaws.services.lexmodelbuilding.model.PutIntentRequest;
import com.amazonaws.services.lexmodelbuilding.model.PutIntentResult;
import com.amazonaws.services.lexmodelbuilding.model.PutSlotTypeRequest;
import com.amazonaws.services.lexmodelbuilding.model.PutSlotTypeResult;
import com.amazonaws.services.lexmodelbuilding.model.Slot;
import com.amazonaws.services.lexmodelbuilding.model.SlotConstraint;
import com.amazonaws.services.lexmodelbuilding.model.Statement;

public class LexFunctionsModel {

	public AmazonLexModelBuilding botBuilder = AmazonLexModelBuildingClientBuilder.standard()
			.withRegion(Regions.EU_WEST_1).build();

	protected PutSlotTypeResult createSlots(Map<String, List<String>> mainslotmap, String BotName)
			throws ParseException {
		PutSlotTypeResult SlotResult = null;

		Iterator<Map.Entry<String, List<String>>> itr = mainslotmap.entrySet().iterator();
		while (itr.hasNext()) {

			Map.Entry<String, List<String>> itr1 = itr.next();

			EnumerationValue enumValue;
			List<EnumerationValue> enumerationValues = new ArrayList<>();
			for (int i = 0; i < itr1.getValue().size(); i++) {
				enumValue = new EnumerationValue().withValue(itr1.getValue().get(i));
				enumerationValues.add(enumValue);
			}

			boolean checkSlotExist = checkSlots(itr1.getKey());
			if (!checkSlotExist) {
				updateSlot(itr1.getKey(), enumerationValues);

			} else {

				PutSlotTypeRequest PutSlotTypeRequest = new PutSlotTypeRequest().withDescription("contains slots")
						.withName(itr1.getKey()).withEnumerationValues(enumerationValues);

				SlotResult = botBuilder.putSlotType(PutSlotTypeRequest);

				System.out.println(SlotResult.getName());
			}
		}

		return SlotResult;
	}

	private void updateSlot(String oneSlot, List<EnumerationValue> enumerationValues) {
		try {
			PutSlotTypeRequest PutSlotTypeRequest = new PutSlotTypeRequest().withDescription("contains slots")
					.withName(oneSlot).withChecksum(getSlotCheckSum(oneSlot)).withEnumerationValues(enumerationValues);

			PutSlotTypeResult SlotResult = botBuilder.putSlotType(PutSlotTypeRequest);

		} catch (com.amazonaws.services.lexmodelbuilding.model.ConflictException ex) {
			updateSlot(oneSlot, enumerationValues);
		}

	}

	protected ArrayList<Intent> createIntents(Map<String, List<String>> mainslotmap, String BotName)
			throws ParseException {

		String slotConstraint = "Optional";

		ArrayList<String> slotList = new ArrayList<String>();
		ArrayList<Intent> intents = new ArrayList<Intent>();
		Iterator<Map.Entry<String, List<String>>> itr = mainslotmap.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry<String, List<String>> itr1 = itr.next();

			Intent test = new Intent().withIntentName(itr1.getKey().toString()).withIntentVersion("$LATEST");
			intents.add(test);
			boolean checkIntentExist = checkIntent(itr1.getKey());

			if (!checkIntentExist) {
				PutIntentRequest putIntentRequest = new PutIntentRequest().withName(itr1.getKey().toString())
						.withDescription("this is a test")
						.withFulfillmentActivity(
								new FulfillmentActivity().withType(FulfillmentActivityType.ReturnIntent))
						.withSampleUtterances(itr1.getValue());

				ExcelToLexLoader load = new ExcelToLexLoader();
				slotList = load.getSlotType(itr1.getValue());

				if (slotList.isEmpty()) {
					PutIntentResult intentResult = botBuilder.putIntent(putIntentRequest);
					System.out.println(intentResult.getName());

				} else {
					for (String slot : slotList) {
						System.out.println("======" + slot);
						List<Slot> slots = new ArrayList<Slot>();
						putIntentRequest.withSlots(createSlotValue(slot));
						slots.clear();

					}

					PutIntentResult intentResult = botBuilder.putIntent(putIntentRequest);
					System.out.println(intentResult.getName());

				}
			} else {
				updateIntent(itr1.getKey(), itr1.getValue());

			}
		}
		return intents;
	}

	private Slot createSlotValue(String slot) {
		
		Slot s1 = new Slot().withDescription("contain slot values").withSlotType(slot)
				.withSlotConstraint(SlotConstraint.Optional)
				.withValueElicitationPrompt(new Prompt().withMaxAttempts(5)
						.withMessages(new Message[] {
								new Message().withContent("tell me again").withContentType(ContentType.PlainText),
								new Message().withContent("tell me again").withContentType(ContentType.PlainText) }))
				.withName(slot);
		s1.withSlotTypeVersion("$LATEST");

		return s1;
	}

	private void updateIntent(String intent, List<String> utterance) {
		
		try {
			List<Slot> slots = new ArrayList<Slot>();
			ArrayList<String> slotList = new ArrayList<String>();
			PutIntentRequest putIntentRequest = new PutIntentRequest().withName(intent.toString())
					.withDescription("this is a test").withChecksum(getIntentChecksum(intent))
					.withFulfillmentActivity(new FulfillmentActivity().withType(FulfillmentActivityType.ReturnIntent))
					.withSampleUtterances(utterance);

			ExcelToLexLoader load = new ExcelToLexLoader();
			slotList = load.getSlotType(utterance);

			if (slotList.isEmpty()) {
				PutIntentResult intentResult = botBuilder.putIntent(putIntentRequest);
				System.out.println(intentResult.getName());
			} else {
				for (String slotOrder : slotList) {
					System.out.println("======" + slotOrder);

					List<Slot> Updateslots = new ArrayList<Slot>();
					putIntentRequest.withSlots(createSlotValue(slotOrder));
					slots.clear();
				}

				PutIntentResult intentResult = botBuilder.putIntent(putIntentRequest);
				System.out.println(intentResult.getName());

			}
		} catch (com.amazonaws.services.lexmodelbuilding.model.ConflictException e) {
			updateIntent(intent, utterance);
		}
	}

	private String getIntentChecksum(String intent) {

		GetIntentRequest req = new GetIntentRequest().withName(intent).withVersion("$LATEST");
		GetIntentResult GetIntentResult = botBuilder.getIntent(req);
		String checksum = GetIntentResult.getChecksum();
		System.out.println(checksum);
		return checksum;

	}

	private String getSlotCheckSum(String slot) {

		GetSlotTypeRequest req = new GetSlotTypeRequest().withName(slot).withVersion("$LATEST");
		GetSlotTypeResult GetSlotResult = botBuilder.getSlotType(req);
		String checksum = GetSlotResult.getChecksum();
		System.out.println(checksum);
		return checksum;

	}

	protected boolean CreateBot(ArrayList<Intent> intentCollection, String BotName, String BotAlias) {

		System.out.println("This is intent list===" + " " + intentCollection);
		System.out.println("This is bot name===" + " " + BotName);
		System.out.println("This is bot alias===" + " " + BotAlias);
		boolean checkBotExist = checkBot(BotName);
		if (checkBotExist) {
			updateBot(BotName, intentCollection);
		} else {
			try {
				PutBotRequest putBotRequest = new PutBotRequest()
						.withChildDirected(
								false)
						.withClarificationPrompt(new Prompt().withMaxAttempts(5)
								.withMessages(new Message[] {
										new Message().withContent("I'm sorry, I can not understand this.")
												.withContentType(ContentType.PlainText),
										new Message().withContent("What is your work")
												.withContentType(ContentType.PlainText) }))
						.withAbortStatement(new Statement().withMessages(new Message[] {
								new Message().withContent("Could you please repeat this?")
										.withContentType(ContentType.PlainText),
								new Message().withContent("What is your work")
										.withContentType(ContentType.PlainText) }))
						.withDescription("bot from excel loader").withIntents(intentCollection).withName(BotName)
						.withVoiceId("Joanna").withLocale("en-US").withProcessBehavior(ProcessBehavior.SAVE)
						.withProcessBehavior(ProcessBehavior.BUILD);

				PutBotResult result = botBuilder.putBot(putBotRequest);
				System.out.println(result.getName());
			}

			catch (BadRequestException | ConflictException | InternalFailureException | LimitExceededException
					| PreconditionFailedException e) {
				System.out.println(e.toString());
			}
		}
		return true;
	}

	private void updateBot(String botName, ArrayList<Intent> intentCollection) {
		PutBotRequest putBotRequest = new PutBotRequest().withChildDirected(false)
				.withClarificationPrompt(new Prompt().withMaxAttempts(5).withMessages(new Message[] {
						new Message().withContent("I'm sorry, I can not understand this.")
								.withContentType(ContentType.PlainText),
						new Message().withContent("What is your work").withContentType(ContentType.PlainText) }))
				.withAbortStatement(new Statement().withMessages(new Message[] {
						new Message().withContent("Could you please repeat this?")
								.withContentType(ContentType.PlainText),
						new Message().withContent("What is your work").withContentType(ContentType.PlainText) }))
				.withDescription("bot from excel loader").withIntents(intentCollection)
				.withChecksum(getBotChecksum(botName)).withName(botName).withVoiceId("Joanna").withLocale("en-US")
				.withProcessBehavior(ProcessBehavior.SAVE).withProcessBehavior(ProcessBehavior.BUILD);

		PutBotResult result = botBuilder.putBot(putBotRequest);
		System.out.println(result.getName());

	}

	private String getBotChecksum(String botName) {
		GetBotRequest req = new GetBotRequest().withName(botName).withVersionOrAlias("$LATEST");

		GetBotResult GetBotResult = botBuilder.getBot(req);
		String checksum = GetBotResult.getChecksum();
		System.out.println(checksum);
		return checksum;
	}

	protected boolean createBotAlias(String BotName, String BotAlias) {
		
		if (BotAliasExist(BotName, BotAlias)) {

			updateBotAlias(BotName, BotAlias);

		} else {

			PutBotAliasRequest PutBotAliasRequest = new PutBotAliasRequest().withBotName(BotName).withName(BotAlias)
					.withChecksum(null).withDescription("its a custom bot").withBotVersion("$LATEST");
			PutBotAliasResult PutBotAliasResult = botBuilder.putBotAlias(PutBotAliasRequest);
			System.out.println(PutBotAliasResult.getName());
		}

		return true;
	}

	private void updateBotAlias(String botName, String botAlias) {
		PutBotAliasRequest PutBotAliasRequest = new PutBotAliasRequest().withBotName(botName).withName(botAlias)
				.withChecksum(getBotAliasChecksum(botName, botAlias)).withDescription("its a custom bot")
				.withBotVersion("$LATEST");
		PutBotAliasResult PutBotAliasResult = botBuilder.putBotAlias(PutBotAliasRequest);
		System.out.println(PutBotAliasResult.getName());

	}

	private String getBotAliasChecksum(String botName, String botAlias) {
		GetBotAliasRequest req = new GetBotAliasRequest().withName(botAlias).withBotName(botName);

		GetBotAliasResult GetBotAliasResult = botBuilder.getBotAlias(req);
		String checksum = GetBotAliasResult.getChecksum();
		System.out.println(checksum);
		return checksum;
	}

	public boolean checkSlots(String slot) {
		try {
			GetSlotTypeRequest req = new GetSlotTypeRequest().withName(slot).withVersion("$LATEST");

			GetSlotTypeResult GetSlotTypeResult = botBuilder.getSlotType(req);
			if (GetSlotTypeResult.getName().equals(slot))
				return false;
			else
				return true;

		} catch (com.amazonaws.services.lexmodelbuilding.model.NotFoundException e) {
			return true;
		}
	}

	public boolean checkIntent(String intent) throws ParseException {

		try {
			GetIntentRequest req = new GetIntentRequest().withName(intent).withVersion("$LATEST");
			GetIntentResult GetIntentResult = botBuilder.getIntent(req);
			if (GetIntentResult.getName().equals(intent))
				return true;
			else
				return false;

		} catch (com.amazonaws.services.lexmodelbuilding.model.NotFoundException e) {

			return false;
		}

	}

	public boolean checkBot(String botName) {
		try {
			GetBotRequest req = new GetBotRequest().withName(botName).withVersionOrAlias("$LATEST");

			GetBotResult GetBotResult = botBuilder.getBot(req);

			if (GetBotResult.getName().equals(botName))
				return true;
			else
				return false;
		} catch (com.amazonaws.services.lexmodelbuilding.model.NotFoundException e) {
			return false;
		}

	}

	private boolean BotAliasExist(String botName, String botAlias) {
		System.out.println("enter check bot aliase");
		try {
			GetBotAliasRequest req = new GetBotAliasRequest().withName(botAlias).withBotName(botName);
			GetBotAliasResult GetBotAliasResult = botBuilder.getBotAlias(req);
			if (GetBotAliasResult.getName().equals(botAlias)) {
				System.out.println("botAlias exist" + " " + botAlias);
				return true;
			} else {
				System.out.println("botAlias not exist" + botAlias);
				return false;
			}
		} catch (com.amazonaws.services.lexmodelbuilding.model.NotFoundException e) {
			System.out.println("no bot aliase found");
			return false;
		}

	}

}

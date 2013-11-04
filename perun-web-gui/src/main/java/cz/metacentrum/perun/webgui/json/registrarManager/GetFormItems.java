package cz.metacentrum.perun.webgui.json.registrarManager;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import cz.metacentrum.perun.webgui.client.PerunWebSession;
import cz.metacentrum.perun.webgui.client.applicationresources.RegistrarFormItemGenerator;
import cz.metacentrum.perun.webgui.client.localization.ButtonTranslation;
import cz.metacentrum.perun.webgui.client.resources.PerunEntity;
import cz.metacentrum.perun.webgui.client.resources.SmallIcons;
import cz.metacentrum.perun.webgui.json.*;
import cz.metacentrum.perun.webgui.model.ApplicationFormItem;
import cz.metacentrum.perun.webgui.model.ApplicationFormItemData;
import cz.metacentrum.perun.webgui.model.ItemTexts;
import cz.metacentrum.perun.webgui.model.PerunError;
import cz.metacentrum.perun.webgui.tabs.registrartabs.EditFormItemTabItem;
import cz.metacentrum.perun.webgui.widgets.AjaxLoaderImage;
import cz.metacentrum.perun.webgui.widgets.Confirm;
import cz.metacentrum.perun.webgui.widgets.CustomButton;

import java.util.ArrayList;

/**
 * Returns the form elements in application form
 * 
 * @author Vaclav Mach <374430@mail.muni.cz>
 * @version $Id$
 */
public class GetFormItems implements JsonCallback {

	// Session
	private PerunWebSession session = PerunWebSession.getInstance();
	
	// VO/GROUP id
	private int id;
	
	// JSON URL
	static private final String JSON_URL = "registrarManager/getFormItems";
	
	// External events
	private JsonCallbackEvents events = new JsonCallbackEvents();
	
	// Type
	private String type = "";
	
	// Loader image
	private AjaxLoaderImage loaderImage = new AjaxLoaderImage();
	
	// Content
	private SimplePanel contents = new SimplePanel();
	
	// list
	private ArrayList<ApplicationFormItem> applFormItems = new ArrayList<ApplicationFormItem>();
	
	// whether is display "settings" or not
	private boolean settings = false;
	
	private PerunEntity entity;
	
	// RegistrarFormItemGenerators for getting values
	private ArrayList<RegistrarFormItemGenerator> applFormGenerators = new ArrayList<RegistrarFormItemGenerator>();

	/**
	 * Creates a new method instance
     *
     * @param entity entity
	 * @param id entity ID
	 * @param settings Whether to show settings
	 */
	public GetFormItems(PerunEntity entity, int id, boolean settings) {
		this.id = id;
		this.entity = entity;
		this.contents.setWidget(loaderImage);
		this.settings = settings;

        loaderImage.asWidget().getElement().setAttribute("style", "text-align: center; vertical-align: middle");

	}

	/**
	 * Creates a new getResources method instance	
	 *
     * @param entity entity
	 * @param id entity ID
	 * @param settings Whether to show settings
	 * @param events Custom events
	 */
	public GetFormItems(PerunEntity entity, int id, boolean settings, JsonCallbackEvents events) {
		this(entity, id, settings);
		this.events = events;
	}

	/**
	

	/**
	 * Retrieve data from RPC
	 */
	public void retrieveData()
	{
		
		String param = "";
		
		if (entity.equals(PerunEntity.VIRTUAL_ORGANIZATION)) {
			param = "vo=" + this.id;
		} else if (entity.equals(PerunEntity.GROUP)) {
			param = "group=" + this.id;
		}
		
		if(type.length() != 0){
			param += "&type=" + type;
		}
		
		JsonClient js = new JsonClient();
		js.setHidden(true);
		js.retrieveData(JSON_URL, param, this);
	}
	
	/**
	 * Returns contents
	 */
	public Widget getContents()
	{
		return this.contents;		
	}


	/**
	 * Called when an error occurs.
	 */
	public void onError(PerunError error) {
		session.getUiElements().setLogErrorText("Error while loading application form items.");
		events.onError(error);
		loaderImage.loadingError(error);
	}

	/**
	 * Called when loading starts.
	 */
	public void onLoadingStart() {
		session.getUiElements().setLogText("Loading application form items in selected VO started.");
		events.onLoadingStart();
	}

	/**
	 * Called when loading successfully finishes.
	 */
	public void onFinished(JavaScriptObject jso) {
		
		applFormItems.clear();
		applFormItems.addAll(JsonUtils.<ApplicationFormItem>jsoAsList(jso));
		applFormGenerators.clear();
		
		if(settings)
		{
			prepareSettings(applFormItems);
		}else{
			prepareApplicationForm(applFormItems);
		}
		
		
		session.getUiElements().setLogText("Loading application form items in selected VO finished:" + applFormItems.size());
		events.onFinished(jso);
		loaderImage.loadingFinished();
	}
	
	
	/**
	 * Prepares the widgets from the items as A FORM FOR SETTINGS
	 * 
	 * @param items
	 */
	public void prepareSettings(final ArrayList<ApplicationFormItem> items){
		
		// refresh table events
		final JsonCallbackEvents refreshEvents = new JsonCallbackEvents(){
			public void onFinished(JavaScriptObject jso){
				prepareSettings(items);
			}
		};
				
		FlexTable ft = new FlexTable();
		ft.addStyleName("borderTable");
		ft.setWidth("100%");
		ft.setCellPadding(8);
		FlexCellFormatter fcf = ft.getFlexCellFormatter();
		
		ft.setHTML(0, 0, "<strong>Short name</strong>");
		ft.setHTML(0, 1, "<strong>Type</strong>");
		ft.setHTML(0, 2, "<strong>Preview</strong>");
		ft.setHTML(0, 3, "<strong>Edit</strong>");
		
		fcf.setStyleName(0, 0, "GPBYFDEFD");
		fcf.setStyleName(0, 1, "GPBYFDEFD");
		fcf.setStyleName(0, 2, "GPBYFDEFD");
		fcf.setStyleName(0, 3, "GPBYFDEFD");
		
		String locale = "en";
		
		if (LocaleInfo.getCurrentLocale().getLocaleName().equals("default") || LocaleInfo.getCurrentLocale().getLocaleName().equals("en")) {
			locale = "en";
		} else {
			locale = "cs";
		}
		
		int i = 1;
		for(final ApplicationFormItem item : items){
			
			final int index = i - 1;
			
			// not yet set locale on config page
			RegistrarFormItemGenerator gen = new RegistrarFormItemGenerator(item, locale);
			
			// 0 = label
			String label = "";
			if(gen.isLabelShown()){
				label = item.getShortname();
			}
			if (item.isRequired() == true) {
				label += "*";
			}
			ft.setHTML(i, 0, label);
			
			// 1 = type
			ft.setHTML(i, 1, item.getType());
			
			// 2 = preview
			Widget w = gen.getWidget();
			ft.setWidget(i, 2, w);
			
			// 3 = EDIT
			FlexTable editTable = new FlexTable();
			editTable.setStyleName("noBorder");
			ft.setWidget(i, 3, editTable);

            // color for items with unsaved changes
            if (item.wasEdited() == true) {
                ft.getFlexCellFormatter().setStyleName(i, 0, "log-changed");
                ft.getFlexCellFormatter().setStyleName(i, 1, "log-changed");
                ft.getFlexCellFormatter().setStyleName(i, 2, "log-changed");
                ft.getFlexCellFormatter().setStyleName(i, 3, "log-changed");
            }
			
			// mark row for deletion
			if (item.isForDelete()) {
				
				ft.getFlexCellFormatter().setStyleName(i, 0, "log-error");
				ft.getFlexCellFormatter().setStyleName(i, 1, "log-error");
				ft.getFlexCellFormatter().setStyleName(i, 2, "log-error");
				ft.getFlexCellFormatter().setStyleName(i, 3, "log-error");

				// undelete button
				CustomButton undelete = new CustomButton(ButtonTranslation.INSTANCE.undeleteFormItemButton(), ButtonTranslation.INSTANCE.undeleteFormItem(), SmallIcons.INSTANCE.arrowLeftIcon(), new ClickHandler(){
					public void onClick(ClickEvent event) {
						items.get(index).setForDelete(false);
						// refresh
						prepareSettings(items);
					}
				});
				
				FlexTable undelTable = new FlexTable();
				undelTable.setStyleName("noBorder");
				undelTable.setHTML(0, 0, "<strong><span style=\"color:red;\">MARKED FOR DELETION</span></strong>");
				undelTable.setWidget(0, 1, undelete);
				ft.setWidget(i, 3, undelTable);
				
			}

            // color for new items to be saved
			if (item.getId() == 0) {
				ft.getFlexCellFormatter().setStyleName(i, 0, "log-success");
				ft.getFlexCellFormatter().setStyleName(i, 1, "log-success");
				ft.getFlexCellFormatter().setStyleName(i, 2, "log-success");
				ft.getFlexCellFormatter().setStyleName(i, 3, "log-success");
			}

			// up
			PushButton upButton = new PushButton(new Image(SmallIcons.INSTANCE.arrowUpIcon()), new ClickHandler() {
				
				public void onClick(ClickEvent event) {

					if(index - 1 < 0) return;
					
					// move it
					items.remove(index);
					items.add(index - 1, item);
					item.setOrdnum(item.getOrdnum()-1);

                    item.setEdited(true);

					// refresh
					prepareSettings(items);
				}
			});
			editTable.setWidget(0, 0, upButton);
            upButton.setTitle(ButtonTranslation.INSTANCE.moveFormItemUp());
			
			// down
			PushButton downButton = new PushButton(new Image(SmallIcons.INSTANCE.arrowDownIcon()), new ClickHandler() {
				
				public void onClick(ClickEvent event) {
					
					if(index + 1 >= items.size()) return;
					
					// move it
					items.remove(index);
					items.add(index + 1, item);
					item.setOrdnum(item.getOrdnum()+1);

                    item.setEdited(true);
					
					// refresh
					prepareSettings(items);
				}
			});
			editTable.setWidget(0, 1, downButton);
            downButton.setTitle(ButtonTranslation.INSTANCE.moveFormItemDown());

			// edit
			CustomButton editButton = new CustomButton(ButtonTranslation.INSTANCE.editFormItemButton(), ButtonTranslation.INSTANCE.editFormItem(), SmallIcons.INSTANCE.applicationFormEditIcon());
			editButton.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					session.getTabManager().addTabToCurrentTab(new EditFormItemTabItem(item, refreshEvents));
                }
			});
			editTable.setWidget(0, 2, editButton);
						
			// remove
			CustomButton removeButton = new CustomButton(ButtonTranslation.INSTANCE.deleteButton(), ButtonTranslation.INSTANCE.deleteFormItem(), SmallIcons.INSTANCE.deleteIcon());
			removeButton.addClickHandler(new ClickHandler() {
				
				public void onClick(ClickEvent event) {
					HTML text = new HTML("<p>Deleting of form items is <strong>NOT RECOMMENDED!</strong><p>You will loose access to data users submitted in older applications within this form item!<p>Do you want to continue?");
					Confirm c = new Confirm("Delete confirm", text, new ClickHandler(){
						public void onClick(ClickEvent event) {
							// mark for deletion when save changes
							items.get(index).setForDelete(true);
							// remove if newly created
							if (items.get(index).getId()==0) {
								items.remove(index);
							}
							// refresh
							prepareSettings(items);
						}
					}, true);
                    c.setNonScrollable(true);
					c.show();
				}
			});
			editTable.setWidget(0, 3, removeButton);
			
			
			// format 
			fcf.setHeight(i, 0, "28px");
			fcf.setVerticalAlignment(i, 0, HasVerticalAlignment.ALIGN_MIDDLE);
			fcf.setVerticalAlignment(i, 1, HasVerticalAlignment.ALIGN_MIDDLE);
			fcf.setVerticalAlignment(i, 2, HasVerticalAlignment.ALIGN_MIDDLE);
	
			i++;
			
		}
		
		contents.setWidget(ft);
	}
	
	
	/**
	 * Prepares the widgets from the items as A DISPLAY FOR THE USER
     *  DEPRECATED: Use GetFormItemsWithPrefilledValues instead 
	 * 
	 * @param items
	 * @deprecated
	 */
	public void prepareApplicationForm(final ArrayList<ApplicationFormItem> items){
		
		FlexTable ft = new FlexTable();
		FlexCellFormatter fcf = ft.getFlexCellFormatter();
		String locale = "en";
		
		if (LocaleInfo.getCurrentLocale().getLocaleName().equals("default")) {
			locale = "en";
		} else {
			locale = "cs";
		}
		
		int i = 0;
		for(final ApplicationFormItem item : items){
			
			String value = "";
			if(item.getShortname().equals("affiliation") || item.getShortname().equals("mail") || item.getShortname().equals("displayName")){
				 value = "from federation";
			}
			
			RegistrarFormItemGenerator gen = new RegistrarFormItemGenerator(item, value, locale);
			this.applFormGenerators.add(gen);
			
			
			if(!gen.isVisible()){
				continue;
			}
			
			
			ItemTexts itemTexts = item.getItemTexts(locale);
			
			
			// WITH LABEL (input box ...)
			if(gen.isLabelShown()){
				
				// 0 = label
				ft.setHTML(i, 0, "<strong>" + gen.getLabelOrShortname() + "</strong>");
								
				// 1 = widget
				Widget w = gen.getWidget();
				w.setTitle(itemTexts.getHelp());
				ft.setWidget(i, 1, w);
			
			// ELSE HTML COMMENT
			}else{
				
				ft.setWidget(i, 0, gen.getWidget());
				
				// colspan = 2
				fcf.setColSpan(i, 0, 2);
			}
			
			// format 
			fcf.setHeight(i, 0, "35px");
			fcf.setVerticalAlignment(i, 0, HasVerticalAlignment.ALIGN_TOP);
			fcf.setVerticalAlignment(i, 1, HasVerticalAlignment.ALIGN_MIDDLE);
	
			i++;
			
		}
		
		contents.setWidget(ft);
	}
	
	/**
	 * Generates the values from the form
	 * @return
	 */
	public ArrayList<ApplicationFormItemData> getValues()
	{
		ArrayList<ApplicationFormItemData> formItemDataList = new ArrayList<ApplicationFormItemData>();
		
		// goes through all the item generators and retrieves the value
		for(RegistrarFormItemGenerator gen : applFormGenerators){
			
			String value = gen.getValue();
			String prefilled = gen.getPrefilledValue();
			JSONObject formItemJSON = new JSONObject(gen.getFormItem());
			
			// remove text (locale), saves data transfer & removes problem with parsing locale
			formItemJSON.put("i18n", new JSONObject());
			
			// cast form item back
			ApplicationFormItem formItem = formItemJSON.getJavaScriptObject().cast();
			
			// prepare package with data
			ApplicationFormItemData data = ApplicationFormItemData.construct(formItem, formItem.getShortname(), value, prefilled, "");
			
			formItemDataList.add(data);
		}
		return formItemDataList;
	}
	
	
	
	

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public ArrayList<ApplicationFormItem> getList() {
		return this.applFormItems;
	}




}
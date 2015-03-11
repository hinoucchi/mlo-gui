/**
 * MloUpdateViewController.java
 * (C) 2014, Hitachi Solutions, Ltd.
 */
package org.o3project.mlo.client.impl.control;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.o3project.mlo.client.control.ClientConfigConstants;
import org.o3project.mlo.client.control.MloDialogController;
import org.o3project.mlo.client.control.MloInputDataException;
import org.o3project.mlo.client.control.MloViewController;
import org.o3project.mlo.client.control.SliceTaskFactory;
import org.o3project.mlo.client.view.FlowPanel;
import org.o3project.mlo.client.view.MloCommonTable;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.o3project.mlo.server.dto.FlowDto;
import org.o3project.mlo.server.dto.SliceDto;

/**
 * スライス更新画面のコントローラクラスです。
 */
public class MloUpdateViewController implements MloViewController, Initializable {
	private static final Log LOG = LogFactory.getLog(MloUpdateViewController.class);

	private static final String SLICE_PARAM_KEY_ID = "Id";
	private static final String SLICE_PARAM_KEY_NAME = "Name";
	
	private static final double PROP_TABLE_WIDTH = 740d;
	private static final double PROP_TABLE_HEIGHT = 85d;
	
	@FXML
	private AnchorPane anchorPane;
	
	@FXML
	private Label headerLabel;

	@FXML
	private ScrollPane scrollPane;
	
	@FXML
	private Pane propPanel;
	
	@FXML
	private Label copyrightLabel;
	
	/**
	 * スライス情報テーブル
	 */
	public MloCommonTable slicePropsDispTable;
	
	private final VBox sliceFlowsDispVBox = new VBox();
	
	private SliceDto targetSlice;
	
	private FlowDto defaultFlowDto;

	/* (non-Javadoc)
	 * @see javafx.fxml.Initializable#initialize(java.net.URL, java.util.ResourceBundle)
	 */
	@Override
	public void initialize(URL url, ResourceBundle br) {
		headerLabel.setText(ClientConfigConstants.APP_NAME);
		copyrightLabel.setText(ClientConfigConstants.APP_COPYRIGHT);
		scrollPane.setContent(sliceFlowsDispVBox);
		
		setUpView();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.o3project.mlo.client.control.MloViewController#getRoot()
	 */
	@Override
	public Parent getRoot() {
		return anchorPane;
	}

	/**
	 * ビュー要素のセットアップを行います。
	 */
	public void setUpView() {
		targetSlice = null;
		setUpSlicePropsView(targetSlice);
		setUpSliceFlowsView(targetSlice);
	}
	
	/**
	 * スライス情報を設定します。
	 * @param slicedto スライス情報 DTO
	 */
	public void setData(SliceDto slicedto){
		targetSlice = slicedto;
		setUpSlicePropsView(targetSlice);
		setUpSliceFlowsView(targetSlice);
	}
	
	/**
	 * 引数で指定されたスライスのプロパティを表示する。
	 * スライスが null の場合は空表示にする。
	 * @param sliceDto 表示するスライス DTO
	 */
	private void setUpSlicePropsView(SliceDto sliceDto) {
		Pane basePane = propPanel;
		
		if (slicePropsDispTable != null) {
			basePane.getChildren().removeAll(slicePropsDispTable);
			slicePropsDispTable.dispose();
			slicePropsDispTable = null;
		}
		
		if (sliceDto != null) {
			LinkedHashMap<String, String> sliceInfoMap = new LinkedHashMap<String, String>();
			sliceInfoMap.put(SLICE_PARAM_KEY_ID, String.valueOf(sliceDto.id));
			sliceInfoMap.put(SLICE_PARAM_KEY_NAME, sliceDto.name);
			slicePropsDispTable = new MloCommonTable(sliceInfoMap, PROP_TABLE_WIDTH, false);
			slicePropsDispTable.setPrefSize(PROP_TABLE_WIDTH, PROP_TABLE_HEIGHT);
			basePane.getChildren().add(slicePropsDispTable);
		}
	}

	/**
	 * 引数で指定されたスライスのフローを表示する。
	 * スライスが null の場合は空表示にする。
	 * @param sliceDto 表示するスライス DTO
	 */
	private void setUpSliceFlowsView(SliceDto sliceDto) {
		Pane basePane = sliceFlowsDispVBox;
		
		FlowPanel[] fps = basePane.getChildren().toArray(new FlowPanel[0]);
		basePane.getChildren().removeAll(fps);
		for (FlowPanel fp : fps) {
			fp.dispose();
		}
		
		if (sliceDto != null) {
			int flowCnt = 1;
			for(FlowDto flowDto : sliceDto.flows){
				FlowPanel flowPanel = new FlowPanel("Flow-"+flowCnt, flowDto, true, defaultFlowDto);
				sliceFlowsDispVBox.getChildren().add(flowPanel);
				flowCnt += 1;
			}
		}
	}
	
	/**
	 * 画面の入力内容からスライス DTO を取得します。
	 * @return スライス DTO
	 * @throws MloInputDataException 入力異常
	 */
	public SliceDto getData() throws MloInputDataException{
		LOG.info("getData() called.");
		SliceDto sliceDto= new SliceDto();
		sliceDto.id = targetSlice.id;
		List<FlowDto> flowList = new ArrayList<FlowDto>();
		
		// フローごとに判定処理
		FlowPanel[] flowPanels = sliceFlowsDispVBox.getChildren().toArray(new FlowPanel[0]);
		for(FlowPanel flowPanel : flowPanels){
			String flowType = flowPanel.getFlowType();
			
			if(FlowPanel.FLOW_TYPE_ADD.equals(flowType)){
				flowList.add(flowPanel.getCreateFlowDto());
			}else if(FlowPanel.FLOW_TYPE_UPDATE.equals(flowType)){
				flowList.add(flowPanel.getUpdateFlowDto());
			}else if(FlowPanel.FLOW_TYPE_DELETE.equals(flowType)){
				flowList.add(flowPanel.getDeleteFlowDto());
			}
			
		}
		sliceDto.flows = flowList;
		
		return sliceDto;
	}
	
	/**
	 * デフォルトフロー情報を設定します。
	 * @param flowDto デフォルトフロー情報
	 */
	public void setDefaultFlowDto(FlowDto flowDto){
		defaultFlowDto = flowDto;
	}

	/**
	 * スライス更新画面のボタンのアクションハンドラです。
	 * FXML からのハンドラ指定用です。
	 * @param event アクションイベント
	 */
	@FXML
	void handleButtonAction(ActionEvent event) {
		
		Button bt = (Button) event.getSource();
		if("addFlowBt".equals(bt.getId())){
			FlowPanel flowPanel = new FlowPanel("new Flow", defaultFlowDto);
			VBox vb = (VBox) scrollPane.getContent();
			vb.getChildren().add(flowPanel);
		}
		
		if("updateSliceBt".equals(bt.getId())){
			MloDialogController mloDialog = new MloDialogController();
			mloDialog.setMsgLabl(SliceTaskFactory.UPDATE_TASK_LB);
			mloDialog.showDaialog();
		}
		
		if("updateCancelBt".equals(bt.getId())){
			MloDialogController mloDialog = new MloDialogController();
			mloDialog.setMsgLabl(SliceTaskFactory.CANCEL_TASK_LB);
			mloDialog.showDaialog();
		}
	}
}

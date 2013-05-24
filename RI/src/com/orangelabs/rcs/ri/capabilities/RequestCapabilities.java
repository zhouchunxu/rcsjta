/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.ri.capabilities;

import java.util.Set;

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.capability.Capabilities;
import org.gsma.joyn.capability.CapabilityService;
import org.gsma.joyn.capability.ICapabilitiesListener;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Refresh capabilities of a given contact
 * 
 * @author jexa7410
 */
public class RequestCapabilities extends Activity implements JoynServiceListener {
	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();
	
    /**
	 * Capability API
	 */
    private CapabilityService capabilityApi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.capabilities_request);
        
        // Set title
        setTitle(R.string.menu_capabilities);
        
        // Set the contact selector
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        spinner.setAdapter(Utils.createContactListAdapter(this));
        spinner.setOnItemSelectedListener(listenerContact);
        
		// Set button callback
        Button refreshBtn = (Button)findViewById(R.id.refresh_btn);
        refreshBtn.setOnClickListener(btnRefreshListener);
        
        // Disable button until not connected to the service
        refreshBtn.setEnabled(false);
        
        // Instanciate API
        capabilityApi = new CapabilityService(getApplicationContext(), this);
                
        // Connect API
        capabilityApi.connect();
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	// Unregister API listener
        try {
	        capabilityApi.removeCapabilitiesListener(capabilitiesListener);
        } catch(JoynServiceException e) {
        }

        // Disconnect API
    	capabilityApi.disconnect();
    }

    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
        // Update refresh button
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        Button refreshBtn = (Button)findViewById(R.id.refresh_btn);
        if (spinner.getAdapter().getCount() == 0) {
        	 // Disable button if no contact available
            refreshBtn.setEnabled(false);
        } else {
            refreshBtn.setEnabled(true);        	
        }
        
        try {
	        // Register capabilities listener
	        capabilityApi.addCapabilitiesListener(capabilitiesListener);
        } catch(JoynServiceException e) {
	    	e.printStackTrace();
        }
    }
    
    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
		Utils.showMessageAndExit(RequestCapabilities.this, getString(R.string.label_api_disconnected));
    }    
    
    /**
     * Callback called when service is registered to the RCS/IMS platform
     */
    public void onServiceRegistered() {
    	// Not used here
    }
    
    /**
     * Callback called when service is unregistered from the RCS/IMS platform
     */
    public void onServiceUnregistered() {
    	// Not used here
    }    
    
    /**
     * Spinner contact listener
     */
    private OnItemSelectedListener listenerContact = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			try {
		        // Get selected contact
				String contact = getContactAtPosition(position);
				
				// Get current capabilities
				Capabilities currentCapabilities = capabilityApi.getContactCapabilities(contact);
	
				// Display default capabilities
		        displayCapabilities(currentCapabilities);

		        // Update capabilities
				updateCapabilities(contact);
		    } catch(JoynServiceNotAvailableException e) {
		    	e.printStackTrace();
				Utils.showMessageAndExit(RequestCapabilities.this, getString(R.string.label_api_disabled));
		    } catch(JoynServiceException e) {
		    	e.printStackTrace();
				Utils.showMessageAndExit(RequestCapabilities.this, getString(R.string.label_api_failed));
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
		}
	};
	
    /**
     * Returns the contact at given position
     * 
     * @param position Position in the adapter
     * @return Contact
     */
    private String getContactAtPosition(int position) {
	    Spinner spinner = (Spinner)findViewById(R.id.contact);
	    MatrixCursor cursor = (MatrixCursor)spinner.getItemAtPosition(position);
	    return cursor.getString(1);
    }
    
    /**
     * Returns the selected contact
     * 
     * @param position Position in the adapter
     * @return Contact
     */
    private String getSelectedContact() {
	    Spinner spinner = (Spinner)findViewById(R.id.contact);
	    MatrixCursor cursor = (MatrixCursor)spinner.getSelectedItem();
	    return cursor.getString(1);
    }

    /**
     * Request button callback
     */
    private OnClickListener btnRefreshListener = new OnClickListener() {
        public void onClick(View v) {
    		// Update capabilities
    		updateCapabilities(getSelectedContact());
        }
    };

    /**
     * Update capabilities of a given contact
     * 
     * @param contact Contact
     */
    private void updateCapabilities(final String contact) {
        // Display info
        Utils.displayLongToast(RequestCapabilities.this, getString(R.string.label_request_in_background, contact));

        // Start request in background
        Thread t = new Thread() {
    		public void run() {
		    	try {
			        // Request new capabilities 
			        capabilityApi.requestContactCapabilities(contact);
			    } catch(JoynServiceNotAvailableException e) {
			    	e.printStackTrace();
					Utils.showMessageAndExit(RequestCapabilities.this, getString(R.string.label_api_disabled));
			    } catch(JoynServiceException e) {
			    	e.printStackTrace();
					Utils.showMessageAndExit(RequestCapabilities.this, getString(R.string.label_api_failed));
				}
    		}
    	};
    	t.start();
    }
    
    /**
     * Display the capabilities
     * 
     * @param capabilities Capabilities
     */
    private void displayCapabilities(Capabilities capabilities) {
    	CheckBox imageCSh = (CheckBox)findViewById(R.id.image_sharing);
		CheckBox videoCSh = (CheckBox)findViewById(R.id.video_sharing);
		CheckBox ft = (CheckBox)findViewById(R.id.file_transfer);
		CheckBox im = (CheckBox)findViewById(R.id.im);
		TextView extensions = (TextView)findViewById(R.id.extensions);
    	if (capabilities != null) {
    		// Set capabilities
    		imageCSh.setChecked(capabilities.isImageSharingSupported());
    		videoCSh.setChecked(capabilities.isVideoSharingSupported());
    		ft.setChecked(capabilities.isFileTransferSupported());
    		im.setChecked(capabilities.isImSessionSupported());

            // Set extensions
    		extensions.setVisibility(View.VISIBLE);
            String result = "";
            Set<String> extensionList = capabilities.getSupportedExtensions();
	        for(String value : extensionList) {
            	result += value.substring(CapabilityService.EXTENSION_PREFIX_NAME.length()+1) + "\n";
            }
            extensions.setText(result);    		
    	}
    }
    
    /**
     * Capabilities listener
     */
    private ICapabilitiesListener capabilitiesListener = new ICapabilitiesListener.Stub() {
	    /**
	     * Callback called when new capabilities are received for a given contact
	     * 
	     * @param contact Contact
	     * @param capabilities Capabilities
	     */
	    public void onCapabilitiesReceived(final String contact, final Capabilities capabilities) {
			handler.post(new Runnable(){
				public void run(){
					// Check if this intent concerns the current selected contact					
					if (Utils.comparePhoneNumbers(getSelectedContact(), contact)) {
						// Update UI
						displayCapabilities(capabilities);
					}
				}
			});
	    };    
    };
}

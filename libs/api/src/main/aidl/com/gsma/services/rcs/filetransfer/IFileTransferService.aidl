package com.gsma.services.rcs.filetransfer;

import android.net.Uri;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.filetransfer.IFileTransfer;
import com.gsma.services.rcs.filetransfer.IOneToOneFileTransferListener;
import com.gsma.services.rcs.filetransfer.IOneToManyFileTransferListener;
import com.gsma.services.rcs.filetransfer.IGroupFileTransferListener;
import com.gsma.services.rcs.filetransfer.IFileTransferServiceConfiguration;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.ICommonServiceConfiguration;

/**
 * File transfer service API
 */
interface IFileTransferService {

	boolean isServiceRegistered();

	int getServiceRegistrationReasonCode();

	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	int getServiceVersion();

	ICommonServiceConfiguration getCommonConfiguration();

	IFileTransferServiceConfiguration getConfiguration();

	IFileTransfer getFileTransfer(in String transferId);

	IFileTransfer transferFile(in ContactId contact, in Uri file, in boolean attachFileicon);

	IFileTransfer transferFileToMany(in List<ContactId> contacts, in Uri file, in boolean attachFileicon);

	IFileTransfer transferFileToGroupChat(in String chatId, in Uri file, in boolean attachFileicon);

	void markFileTransferAsRead(in String transferId);

	void addEventListener2(in IOneToOneFileTransferListener listener);

	void removeEventListener2(in IOneToOneFileTransferListener listener);

	void addEventListener4(in IOneToManyFileTransferListener listener);

	void removeEventListener4(in IOneToManyFileTransferListener listener);

	void addEventListener3(in IGroupFileTransferListener listener);

	void removeEventListener3(in IGroupFileTransferListener listener);

	boolean isAllowedToTransferFile(in ContactId contact);

	boolean isAllowedToTransferFileToMany(in List<ContactId> contacts);

	boolean isAllowedToTransferFileToGroupChat(in String chatId);

	void deleteOneToOneFileTransfers();

	void deleteOneToManyFileTransfers();

	void deleteGroupFileTransfers();

	void deleteOneToOneFileTransfers2(in ContactId contact);

	void deleteOneToManyFileTransfers2(in String chatId);

	void deleteGroupFileTransfers2(in String chatId);

	void deleteFileTransfer(in String transferId);

	void clearFileTransferDeliveryExpiration(in List<String> transferIds);

	IFileTransfer transferFile2(in ContactId contact, in Uri file, in int disposition, in boolean attachFileicon);

	IFileTransfer transferFileToMany2(in List<ContactId> contacts, in Uri file, in int disposition, in boolean attachFileicon);

	IFileTransfer transferFileToGroupChat2(in String chatId, in Uri file, in int disposition, in boolean attachFileicon);
}
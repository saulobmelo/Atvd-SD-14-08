package monitoring.rmi;

import monitoring.common.ResourceStatus;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GetStatusService extends Remote {
    ResourceStatus getStatus() throws RemoteException;
}
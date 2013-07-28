package it.michelepiccirillo.paperplane.network;

import it.michelepiccirillo.paperplane.domain.Peer;

import java.util.List;

public interface PeerListener {
	void onPeerListChanged(List<Peer> peers);
	void onPeerConnected(Peer p);
}

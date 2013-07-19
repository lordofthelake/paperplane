package it.michelepiccirillo.paperplane;

import java.util.List;

public interface PeerListener {
	void onPeerListChanged(List<Peer> peers);
}

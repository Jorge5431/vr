#include "../common/protocol.h"
#include <cassert>
#include <cstdint>
#include <cstring>

int main() {
    static_assert(sizeof(KbtHandPacket) == 92, "Existing UDP wire size changed");
    KbtHandPacket packet{};
    packet.magic = KBT_HAND_MAGIC;
    packet.flags = KBT_HAND_F_POS | KBT_HAND_F_PINCH | KBT_HAND_F_GRIP;
    packet.side[0].flags = KBT_HS_TRACKED | KBT_HS_GRIP;
    packet.side[1].flags = KBT_HS_TRACKED | KBT_HS_PINCH;
    std::uint8_t wire[92]{};
    std::memcpy(wire, &packet, sizeof wire);
    KbtHandPacket decoded{};
    std::memcpy(&decoded, wire, sizeof wire);
    assert(decoded.magic == KBT_HAND_MAGIC);
    assert((decoded.side[0].flags & KBT_HS_GRIP) != 0);
    assert((decoded.side[0].flags & KBT_HS_PINCH) == 0);
    assert((decoded.side[1].flags & KBT_HS_PINCH) != 0);
    assert((decoded.side[1].flags & KBT_HS_GRIP) == 0);
    // Older packets omit the capability bit and must never activate grip.
    decoded.flags &= ~KBT_HAND_F_GRIP;
    assert(!((decoded.flags & KBT_HAND_F_GRIP) && (decoded.side[0].flags & KBT_HS_GRIP)));
}

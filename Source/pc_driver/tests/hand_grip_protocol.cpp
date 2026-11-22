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
    decoded.flags |= KBT_HAND_F_CURL;
    const uint32_t curls = (0xAu << 20) | (9u << 4) | (4u << 8);
    decoded.side[0].reserved[0] = uint8_t(curls);
    decoded.side[0].reserved[1] = uint8_t(curls >> 8);
    decoded.side[0].reserved[2] = uint8_t(curls >> 16);
    const uint32_t unpacked = uint32_t(decoded.side[0].reserved[0]) |
        (uint32_t(decoded.side[0].reserved[1]) << 8) | (uint32_t(decoded.side[0].reserved[2]) << 16);
    assert((unpacked >> 20) == 0xA);
    assert(((unpacked >> 4) & 15u) == 9u);
    assert(((unpacked >> 8) & 15u) == 4u);
}

"""Create an approximate, metre-scale point 88 mesh from the supplied photographs."""

from math import cos, pi, sin
from pathlib import Path

out = Path(__file__).resolve().parents[1] / "Models"
out.mkdir(exist_ok=True)
obj = ["# Approximate STABILO point 88 blue, based on user photographs", "# Units: metres; writing end/cap points along +Z", "mtllib Stabilo-point88-blue-approx.mtl"]
vertex_count = 0


def vertex(x, y, z):
    global vertex_count
    vertex_count += 1
    obj.append(f"v {x:.6f} {y:.6f} {z:.6f}")
    return vertex_count


def face(indices):
    obj.append("f " + " ".join(str(i) for i in indices))


def prism(name, material, sides, radius, z0, z1, phase=pi / 6):
    obj.extend((f"o {name}", f"usemtl {material}"))
    a = [vertex(radius * cos(2 * pi * i / sides + phase),
                radius * sin(2 * pi * i / sides + phase), z0) for i in range(sides)]
    b = [vertex(radius * cos(2 * pi * i / sides + phase),
                radius * sin(2 * pi * i / sides + phase), z1) for i in range(sides)]
    for i in range(sides):
        j = (i + 1) % sides
        face((a[i], a[j], b[j], b[i]))
    face(tuple(reversed(a)))
    face(tuple(b))


# Overall length 160 mm, with a 27 mm cap and a 7 mm blue tail.
prism("blue_tail", "blue", 12, .0036, -.160, -.153)
prism("orange_hex_barrel", "orange", 6, .0040, -.153, -.027)
prism("black_cap_seam", "dark", 12, .0045, -.0275, -.0268)
prism("long_blue_cap", "blue", 6, .0048, -.027, 0)

# Thin white longitudinal lines on two opposite flats of the orange hexagon.
obj.extend(("o white_barrel_stripes", "usemtl white"))
for side in (0, 3):
    a = 2 * pi * side / 6 + pi / 6
    b = 2 * pi * (side + 1) / 6 + pi / 6
    mx = .004 * (cos(a) + cos(b)) / 2
    my = .004 * (sin(a) + sin(b)) / 2
    tangent_x = cos(b) - cos(a)
    tangent_y = sin(b) - sin(a)
    scale = .00035 / (tangent_x**2 + tangent_y**2) ** .5
    p = (mx - tangent_x * scale, my - tangent_y * scale)
    q = (mx + tangent_x * scale, my + tangent_y * scale)
    face((vertex(*p, -.151), vertex(*q, -.151),
          vertex(*q, -.028), vertex(*p, -.028)))

(out / "Stabilo-point88-blue-approx.obj").write_text("\n".join(obj) + "\n", encoding="utf-8")
(out / "Stabilo-point88-blue-approx.mtl").write_text(
    "newmtl orange\nKd 0.98 0.52 0.02\n"
    "newmtl blue\nKd 0.04 0.16 0.69\n"
    "newmtl white\nKd 0.96 0.96 0.93\n"
    "newmtl dark\nKd 0.03 0.03 0.04\n", encoding="utf-8")

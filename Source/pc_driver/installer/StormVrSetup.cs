using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Reflection;
using System.Windows.Forms;
using Microsoft.Win32;

internal static class StormVrSetup {
    const string Title = "Instalação do StormVR para SteamVR";
    static readonly string[] Required = {
        "driver.vrdrivermanifest", @"bin\win64\driver_stormvr.dll", @"bin\win64\stormvr_config.exe",
        @"bin\win64\openvr_api.dll", @"bin\win64\ViGEmClient.dll", @"Gamepad\stormvr_gamepad.exe",
        @"Gamepad\ViGEmClient.dll", @"resources\settings\default.vrsettings", @"resources\input\stormvr_generic_profile.json"
    };

    static bool HasRuntime(string root) {
        return !String.IsNullOrEmpty(root) && Directory.Exists(Path.Combine(root, "drivers"))
            && File.Exists(Path.Combine(root, @"bin\win64\vrserver.exe"));
    }

    static string FindSteamVr() {
        foreach (var view in new[] { RegistryView.Registry64, RegistryView.Registry32 }) {
            using (var hklm = RegistryKey.OpenBaseKey(RegistryHive.LocalMachine, view))
            using (var key = hklm.OpenSubKey(@"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\Steam App 250820")) {
                var dir = key == null ? null : key.GetValue("InstallLocation") as string;
                if (HasRuntime(dir)) return dir;
            }
        }
        using (var key = Registry.CurrentUser.OpenSubKey(@"Software\Valve\Steam")) {
            var dir = key == null ? null : key.GetValue("SteamPath") as string;
            if (dir != null) {
                var path = Path.Combine(dir, @"steamapps\common\SteamVR");
                if (HasRuntime(path)) return path;
            }
        }
        return "";
    }

    static void CheckDirectory(string path) {
        if (Directory.Exists(path) && (File.GetAttributes(path) & FileAttributes.ReparsePoint) != 0)
            throw new IOException("Esta pasta é um redirecionamento. Use o caminho físico da biblioteca: " + path);
    }

    static void Unpack(string target) {
        using (var blob = Assembly.GetExecutingAssembly().GetManifestResourceStream("driver.zip"))
        using (var zip = new ZipArchive(blob, ZipArchiveMode.Read)) {
            var seen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            var prefix = Path.GetFullPath(target).TrimEnd('\\') + "\\";
            foreach (var file in zip.Entries) {
                var name = file.FullName.Replace('/', '\\');
                if (!name.StartsWith("stormvr\\", StringComparison.OrdinalIgnoreCase) || name.EndsWith("\\")) continue;
                var path = Path.GetFullPath(Path.Combine(target, name.Substring("stormvr\\".Length)));
                if (!path.StartsWith(prefix, StringComparison.OrdinalIgnoreCase) || !seen.Add(path))
                    throw new InvalidDataException("Caminho inválido no pacote.");
                Directory.CreateDirectory(Path.GetDirectoryName(path));
                using (var stream = file.Open())
                using (var output = new FileStream(path, FileMode.CreateNew, FileAccess.Write, FileShare.None)) {
                    stream.CopyTo(output);
                    if (output.Length != file.Length) throw new InvalidDataException("Arquivo incompleto em " + name);
                }
            }
            foreach (var name in Required)
                if (!File.Exists(Path.Combine(target, name))) throw new InvalidDataException("Falta " + name);
        }
    }

    internal static string Install(string selectedRoot) {
        var root = Path.GetFullPath(selectedRoot);
        if (!HasRuntime(root)) throw new DirectoryNotFoundException("Selecione a pasta do SteamVR que contém drivers e bin\\win64\\vrserver.exe.");
        if (Process.GetProcessesByName("vrserver").Length != 0 || Process.GetProcessesByName("vrmonitor").Length != 0)
            throw new IOException("Feche o SteamVR antes de instalar o driver.");
        var drivers = Path.Combine(root, "drivers");
        var target = Path.Combine(drivers, "stormvr");
        var stash = Path.Combine(root, "StormVR-backups");
        CheckDirectory(drivers); CheckDirectory(target); CheckDirectory(stash);
        if (Directory.Exists(target) && !File.Exists(Path.Combine(target, "driver.vrdrivermanifest")))
            throw new IOException("A pasta de destino já existe e não contém um manifesto de driver.");
        var nonce = Guid.NewGuid().ToString("N");
        var stage = Path.Combine(root, "StormVR-stage-" + nonce);
        if (Directory.Exists(stage)) throw new IOException("A pasta temporária já existe.");
        Directory.CreateDirectory(stage);
        string previous = null;
        try {
            Unpack(stage);
            if (Directory.Exists(target)) {
                Directory.CreateDirectory(stash);
                var backup = Path.Combine(stash, DateTime.Now.ToString("yyyyMMdd-HHmmss") + "-" + nonce.Substring(0, 8));
                Directory.Move(target, backup);
                previous = backup;
            }
            try { Directory.Move(stage, target); }
            catch {
                if (previous != null && !Directory.Exists(target)) Directory.Move(previous, target);
                throw;
            }
        }
        finally {
            if (Directory.Exists(stage)) Directory.Delete(stage, true);
        }
        return previous;
    }

    [STAThread] static int Main(string[] args) {
        Application.EnableVisualStyles();
        var root = args.Length == 2 && args[0] == "--library" ? args[1] : FindSteamVr();
        try {
            using (var picker = new FolderBrowserDialog()) {
                picker.Description = "Escolha a pasta SteamVR. Feche o SteamVR antes de continuar.";
                picker.SelectedPath = root;
                picker.ShowNewFolderButton = false;
                if (picker.ShowDialog() != DialogResult.OK) return 0;
                root = picker.SelectedPath;
            }
            if (!HasRuntime(root)) throw new IOException("A pasta selecionada não contém o SteamVR para Windows x64.");
            if (MessageBox.Show("Instalar o driver, o painel e o Gamepad em\n" + Path.Combine(root, @"drivers\stormvr") +
                "?\n\nUma instalação anterior será guardada em StormVR-backups na pasta do SteamVR.",
                Title, MessageBoxButtons.OKCancel, MessageBoxIcon.Information) != DialogResult.OK) return 0;
            var backup = Install(root);
            MessageBox.Show("Instalação concluída. Abra o SteamVR e ative Modo PC na Sala VR do celular." +
                (backup == null ? "" : "\n\nCópia anterior em\n" + backup), Title);
            return 0;
        }
        catch (UnauthorizedAccessException) {
            var identity = System.Security.Principal.WindowsIdentity.GetCurrent();
            var admin = new System.Security.Principal.WindowsPrincipal(identity).IsInRole(System.Security.Principal.WindowsBuiltInRole.Administrator);
            if (!admin && MessageBox.Show("O Windows exige permissão para escrever nesta biblioteca. Continuar como administrador?",
                Title, MessageBoxButtons.YesNo) == DialogResult.Yes) {
                try {
                    Process.Start(new ProcessStartInfo(Application.ExecutablePath, "--library \"" + root.TrimEnd('\\') + "\"") {
                        UseShellExecute = true, Verb = "runas"
                    });
                    return 0;
                } catch (Exception e) { MessageBox.Show(e.Message, Title); }
            } else if (admin) MessageBox.Show("Acesso negado à biblioteca SteamVR selecionada.", Title);
            return 1;
        }
        catch (Exception e) {
            MessageBox.Show(e.Message, Title, MessageBoxButtons.OK, MessageBoxIcon.Error);
            return 1;
        }
    }
}

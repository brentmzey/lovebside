class Bside < Formula
  desc "BSide - Kotlin Multiplatform Messaging App"
  homepage "https://github.com/brentmzey/lovebside"
  version "0.1.0"
  
  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/brentmzey/lovebside/releases/download/v0.1.0/bside-0.1.0-macOS-arm64.dmg"
      sha256 "PLACEHOLDER_SHA256_ARM64"
    else
      url "https://github.com/brentmzey/lovebside/releases/download/v0.1.0/bside-0.1.0-macOS-x86_64.dmg"
      sha256 "PLACEHOLDER_SHA256_X86_64"
    end
  end
  
  on_linux do
    url "https://github.com/brentmzey/lovebside/releases/download/v0.1.0/bside-0.1.0-Linux.deb"
    sha256 "PLACEHOLDER_SHA256_LINUX"
  end
  
  depends_on java: "17+"
  
  def install
    if OS.mac?
      prefix.install Dir["*"]
      bin.install_symlink prefix/"BSide.app/Contents/MacOS/BSide" => "bside"
    else
      # Linux installation
      prefix.install Dir["*"]
      bin.install "bside"
    end
  end
  
  def caveats
    <<~EOS
      BSide has been installed!
      
      To start BSide:
        bside
      
      Or on macOS:
        open -a BSide
      
      Documentation: https://github.com/brentmzey/lovebside/tree/main/docs
      Issues: https://github.com/brentmzey/lovebside/issues
    EOS
  end
  
  test do
    system "#{bin}/bside", "--version"
  end
end

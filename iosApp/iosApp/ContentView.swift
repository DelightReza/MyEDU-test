import SwiftUI
import shared

struct ContentView: View {
    let greeting = Greeting().greet()
    let platformInfo = Greeting().getPlatformInfo()
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Image(systemName: "graduationcap.fill")
                    .font(.system(size: 80))
                    .foregroundColor(.blue)
                    .padding(.top, 50)
                
                Text("MyEDU")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                
                Text(greeting)
                    .font(.title3)
                    .foregroundColor(.secondary)
                    .padding(.top, 10)
                
                Text(platformInfo)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
                
                Spacer()
                
                VStack(spacing: 15) {
                    NavigationButton(title: "Home", icon: "house.fill")
                    NavigationButton(title: "Schedule", icon: "calendar")
                    NavigationButton(title: "Grades", icon: "doc.text.fill")
                    NavigationButton(title: "Profile", icon: "person.fill")
                }
                .padding(.horizontal)
                .padding(.bottom, 50)
            }
            .navigationBarHidden(true)
        }
    }
}

struct NavigationButton: View {
    let title: String
    let icon: String
    
    var body: some View {
        HStack {
            Image(systemName: icon)
                .font(.title2)
                .frame(width: 30)
            Text(title)
                .font(.headline)
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
